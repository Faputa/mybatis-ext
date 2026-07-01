package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import io.github.mybatisext.annotation.TestMode;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.FilterableInfo;
import io.github.mybatisext.metadata.JoinTableInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.PropertyType;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.ognl.Ognl;
import io.github.mybatisext.statement.NestedSelectHelper;
import io.github.mybatisext.util.SimpleStringTemplate;
import io.github.mybatisext.util.StringUtils;

public class ConditionHelper {

    public static Condition buildForConditionList(ConditionList conditionList) {
        List<Condition> andConditions = new ArrayList<>();
        List<Condition> orConditions = new ArrayList<>();
        for (ConditionList list = conditionList; list != null; list = list.getTailList()) {
            andConditions.add(list.getCondition());
            if (list.getLogicalOperator() == LogicalOperator.OR) {
                Condition condition = new Condition(ConditionType.COMPLEX);
                condition.setPropertyInfos(list.getCondition().getPropertyInfos());
                condition.setLogicalOperator(LogicalOperator.AND);
                condition.getSubConditions().addAll(andConditions);
                andConditions.clear();
                orConditions.add(condition);
            }
        }
        if (!andConditions.isEmpty()) {
            Condition condition = new Condition(ConditionType.COMPLEX);
            condition.setPropertyInfos(conditionList.getCondition().getPropertyInfos());
            condition.setLogicalOperator(LogicalOperator.AND);
            condition.getSubConditions().addAll(andConditions);
            orConditions.add(condition);
        }
        Condition condition = new Condition(ConditionType.COMPLEX);
        condition.setPropertyInfos(conditionList.getCondition().getPropertyInfos());
        condition.setLogicalOperator(LogicalOperator.OR);
        condition.getSubConditions().addAll(orConditions);
        return condition;
    }

    public static @Nullable Condition simplifyCondition(Condition condition) {
        if (condition.getTestMode() == TestMode.False && StringUtils.isBlank(condition.getTestTemplate())) {
            return null;
        }
        if (condition.getType() == ConditionType.COMPLEX) {
            if (condition.getSubConditions().size() == 1 && condition.getTestMode() == TestMode.None && StringUtils.isBlank(condition.getTestTemplate())) {
                return simplifyCondition(condition.getSubConditions().iterator().next());
            }
            for (Condition c : new ArrayList<>(condition.getSubConditions())) {
                condition.getSubConditions().remove(c);
                Condition simplifyCondition = simplifyCondition(c);
                if (simplifyCondition == null) {
                    continue;
                }
                condition.getSubConditions().add(simplifyCondition);
            }
            if (condition.getSubConditions().isEmpty()) {
                return null;
            }
            return condition;
        }
        return condition;
    }

    public static Condition buildForTableInfo(TableInfo tableInfo, boolean onlyById, String param) {
        Condition condition = buildForTableInfoInner(tableInfo, onlyById, onlyById, param);
        if (condition.getSubConditions().isEmpty() && onlyById) {
            condition = buildForTableInfoInner(tableInfo, false, true, param);
        }
        return condition;
    }

    private static Condition buildForTableInfoInner(TableInfo tableInfo, boolean onlyById, boolean strictMatch, String param) {
        Condition condition = new Condition(ConditionType.COMPLEX);
        condition.setPropertyInfos(tableInfo.getNameToPropertyInfo());
        condition.setLogicalOperator(LogicalOperator.AND);
        condition.setTestMode(TestMode.None);
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            Condition subCondition = buildForPropertyInfo(tableInfo, propertyInfo, onlyById, strictMatch, param, param);
            if (subCondition == null) {
                continue;
            }
            condition.getSubConditions().add(subCondition);
        }
        return condition;
    }

    private static @Nullable Condition buildForPropertyInfo(TableInfo tableInfo, PropertyInfo propertyInfo, boolean onlyById, boolean strictMatch, String prefix, String param) {
        if (onlyById && (propertyInfo.getPropertyType() == PropertyType.RESULT || !propertyInfo.isOwnColumn())) {
            return null;
        }
        Condition condition;
        if (strictMatch) {
            condition = new Condition(StringUtils.isBlank(propertyInfo.getColumnName()) ? ConditionType.COMPLEX : ConditionType.BASIC);
            condition.setTestMode(propertyInfo.getPropertyType() == PropertyType.COLLECTION ? TestMode.NotEmpty : TestMode.None);
            condition.setCompareOperator(CompareOperator.Equals);
            condition.setLogicalOperator(LogicalOperator.AND);
        } else {
            if (propertyInfo.getFilterableInfo() == null) {
                return null;
            }
            condition = new Condition(StringUtils.isBlank(propertyInfo.getColumnName()) ? ConditionType.COMPLEX : ConditionType.BASIC);
            applyFilterableInfo(condition, propertyInfo.getFilterableInfo(), tableInfo, param);
            if (propertyInfo.getPropertyType() == PropertyType.COLLECTION && (condition.getTestMode() == TestMode.None || condition.getTestMode() == TestMode.NotNull)) {
                condition.setTestMode(TestMode.NotEmpty);
            }
        }
        condition.setPropertyInfos(tableInfo.getNameToPropertyInfo());
        condition.setPropertyInfo(propertyInfo);
        Variable variable = new Variable(prefix, propertyInfo.getName(), propertyInfo.getJavaType());
        if (propertyInfo.getPropertyType() == PropertyType.COLLECTION && condition.getCompareOperator() != CompareOperator.In) {
            condition.setCollectionVariable(variable);
            condition.setVariable(variable.getItemVariable());
        } else {
            condition.setVariable(variable);
        }
        for (PropertyInfo subPropertyInfo : propertyInfo.getNameToPropertyInfo().values()) {
            Condition subCondition = buildForPropertyInfo(tableInfo, subPropertyInfo, onlyById, strictMatch, condition.getVariable().getFullName(), param);
            if (subCondition == null) {
                continue;
            }
            condition.getSubConditions().add(subCondition);
        }
        return condition;
    }

    private static void applyFilterableInfo(Condition condition, FilterableInfo filterableInfo, TableInfo tableInfo, String param) {
        condition.setTestMode(filterableInfo.getTestMode());
        condition.setCompareOperator(filterableInfo.getOperator());
        condition.setLogicalOperator(filterableInfo.getLogicalOperator());
        condition.setIgnorecase(filterableInfo.isIgnorecase());
        condition.setNot(filterableInfo.isNot());
        condition.setTestTemplate(filterableInfo.getTestTemplate());
        condition.setExprTemplate(filterableInfo.getExprTemplate());
        if (condition.getCompareOperator().isRequiredSecondVariable()) {
            PropertyInfo secondPropertyInfo = TableInfoFactory.getPropertyInfo(tableInfo, filterableInfo.getSecondVariable());
            condition.setSecondVariable(new Variable(param, filterableInfo.getSecondVariable(), secondPropertyInfo.getJavaType()));
        }
    }

    public static void collectUsedJoinTableInfo(Condition condition, Collection<JoinTableInfo> joinTableInfos) {
        if (condition.getPropertyInfo() != null && condition.getPropertyInfo().getFetchType() != null) {
            return;
        }
        if (StringUtils.isNotBlank(condition.getExprTemplate())) {
            ConditionHook conditionHook = new ConditionHook(condition);
            SimpleStringTemplate.build(condition.getExprTemplate(), conditionHook, false);
            joinTableInfos.addAll(conditionHook.getUsedJoinTableInfos());
        } else if (condition.getType() == ConditionType.COMPLEX) {
            for (Condition subCondition : condition.getSubConditions()) {
                collectUsedJoinTableInfo(subCondition, joinTableInfos);
            }
        } else {
            condition.getPropertyInfo().collectUsedJoinTableInfo(joinTableInfos);
        }
    }

    public static String toWhere(Condition condition, Dialect dialect) {
        if (condition.hasTest()) {
            return "<where>" + toScript(condition, condition.getLogicalOperator(), dialect) + "</where>";
        }
        if (condition.getType() == ConditionType.COMPLEX) {
            return toWhere(condition.getSubConditions(), condition.getLogicalOperator(), dialect);
        }
        return "WHERE " + toExpr(condition, dialect);
    }

    public static String toWhere(Collection<Condition> conditions, LogicalOperator logicalOperator, Dialect dialect) {
        if (conditions.size() == 1) {
            return toWhere(conditions.stream().findFirst().get(), dialect);
        }
        List<String> ss = new ArrayList<>();
        if (conditions.stream().anyMatch(Condition::hasTest)) {
            for (Condition condition : conditions) {
                ss.add(toScript(condition, logicalOperator, dialect));
            }
            return "<where>" + String.join(" ", ss) + "</where>";
        }
        {
            for (Condition condition : conditions) {
                ss.add(toExpr(condition, dialect));
            }
            return "WHERE " + String.join(" " + logicalOperator + " ", ss);
        }
    }

    public static String toHaving(Condition condition, Dialect dialect) {
        if (condition.hasTest()) {
            return "<trim prefix=\"HAVING\" prefixOverrides=" + condition.getLogicalOperator() + ">" + toScript(condition, condition.getLogicalOperator(), dialect) + "</trim>";
        }
        if (condition.getType() == ConditionType.COMPLEX) {
            return toHaving(condition.getSubConditions(), condition.getLogicalOperator(), dialect);
        }
        return "HAVING " + toExpr(condition, dialect);
    }

    public static String toHaving(Collection<Condition> conditions, LogicalOperator logicalOperator, Dialect dialect) {
        if (conditions.size() == 1) {
            return toHaving(conditions.stream().findFirst().get(), dialect);
        }
        List<String> ss = new ArrayList<>();
        if (conditions.stream().anyMatch(Condition::hasTest)) {
            ss.add("<trim prefix=\"HAVING\" prefixOverrides=" + LogicalOperator.AND + ">");
            for (Condition condition : conditions) {
                ss.add(toScript(condition, LogicalOperator.AND, dialect));
            }
            ss.add("</trim>");
            return String.join(" ", ss);
        }
        {
            for (Condition condition : conditions) {
                ss.add(toExpr(condition, dialect));
            }
            return "HAVING " + String.join(" " + logicalOperator + " ", ss);
        }
    }

    private static String toScript(Condition condition, @Nullable LogicalOperator logicalOperator, Dialect dialect) {
        if (condition.hasTest()) {
            if (condition.getSubConditions().size() == 1 && StringUtils.isBlank(condition.getExprTemplate())) {
                Condition subCondition = condition.getSubConditions().iterator().next();
                if (!subCondition.hasTest()) {
                    return "<if test=\"" + toTestOgnl(condition) + "\">" + toExprWithPrefix(subCondition, logicalOperator, dialect) + "</if>";
                }
            }
            return "<if test=\"" + toTestOgnl(condition) + "\">" + toExprWithPrefix(condition, logicalOperator, dialect) + "</if>";
        }
        return toExprWithPrefix(condition, logicalOperator, dialect);
    }

    private static String toTestOgnl(Condition condition) {
        if (StringUtils.isNotBlank(condition.getTestTemplate())) {
            return SimpleStringTemplate.build(condition.getTestTemplate(), condition);
        }
        Variable variable = condition.getCollectionVariable() != null ? condition.getCollectionVariable() : condition.getVariable();
        if (condition.getTestMode() == TestMode.NotEmpty) {
            return Ognl.IsNotEmpty + "(" + variable + ")";
        }
        if (condition.getTestMode() == TestMode.NotNull) {
            return variable + " != null";
        }
        if (condition.getTestMode() == TestMode.False) {
            return "false";
        }
        return null;
    }

    private static String toExprWithPrefix(Condition condition, @Nullable LogicalOperator logicalOperator, Dialect dialect) {
        String prefix = LogicalOperator.AND == logicalOperator ? "AND " : LogicalOperator.OR == logicalOperator ? "OR " : "";
        if (condition.isNot()) {
            prefix += "NOT ";
        }
        if (StringUtils.isNotBlank(condition.getExprTemplate())) {
            String expr = SimpleStringTemplate.build(condition.getExprTemplate(), condition);
            if (condition.getPropertyInfo() != null && condition.getPropertyInfo().getFetchType() != null) {
                expr = NestedSelectHelper.buildExistSubSelect(condition.getPropertyInfo(), "(" + expr + ")");
            }
            return prefix + expr;
        }
        if (condition.getType() == ConditionType.BASIC) {
            if (condition.getCollectionVariable() != null) {
                List<String> ss = new ArrayList<>();
                if (condition.getPropertyInfo() != null && condition.getPropertyInfo().getFetchType() != null) {
                    ss.add("<foreach collection=\"" + condition.getCollectionVariable() + "\" item=\"" + condition.getVariable() + "\" open=\"" + prefix + NestedSelectHelper.buildExistSubSelect(condition.getPropertyInfo(), "(\" close=\")") + "\" separator=\"OR\">");
                } else {
                    ss.add("<foreach collection=\"" + condition.getCollectionVariable() + "\" item=\"" + condition.getVariable() + "\" open=\"" + prefix + "(\" close=\")\" separator=\"OR\">");
                }
                ss.add(toBasicExpr(condition, condition.getCompareOperator(), condition.isIgnorecase(), dialect));
                ss.add("</foreach>");
                return String.join(" ", ss);
            }
            String expr = toBasicExpr(condition, condition.getCompareOperator(), condition.isIgnorecase(), dialect);
            if (condition.getPropertyInfo() != null && condition.getPropertyInfo().getFetchType() != null) {
                expr = NestedSelectHelper.buildExistSubSelect(condition.getPropertyInfo(), expr);
            }
            return prefix + expr;
        }
        if (condition.getType() == ConditionType.COMPLEX) {
            List<String> ss = new ArrayList<>();
            if (condition.getCollectionVariable() != null) {
                if (condition.getPropertyInfo() != null && condition.getPropertyInfo().getFetchType() != null) {
                    ss.add("<foreach collection=\"" + condition.getCollectionVariable() + "\" item=\"" + condition.getVariable() + "\" open=\"" + prefix + NestedSelectHelper.buildExistSubSelect(condition.getPropertyInfo(), "(\" close=\")") + "\" separator=\"OR\">");
                } else {
                    ss.add("<foreach collection=\"" + condition.getCollectionVariable() + "\" item=\"" + condition.getVariable() + "\" open=\"" + prefix + "(\" close=\")\" separator=\"OR\">");
                }
                ss.add("<trim prefix=\"(\" suffix=\")\" prefixOverrides=\"" + condition.getLogicalOperator() + "\" >");
                for (Condition subCondition : condition.getSubConditions()) {
                    ss.addAll(buildSubConditionExpr(subCondition, condition.getLogicalOperator(), dialect));
                }
                ss.add("</trim>");
                ss.add("</foreach>");
                return String.join(" ", ss);
            }
            if (condition.getPropertyInfo() != null && condition.getPropertyInfo().getFetchType() != null) {
                ss.add("<trim prefix=\"" + prefix + NestedSelectHelper.buildExistSubSelect(condition.getPropertyInfo(), "(\" suffix=\")") + "\" prefixOverrides=\"" + condition.getLogicalOperator() + "\" >");
            } else {
                ss.add("<trim prefix=\"" + prefix + "(\" suffix=\")\" prefixOverrides=\"" + condition.getLogicalOperator() + "\" >");
            }
            for (Condition subCondition : condition.getSubConditions()) {
                ss.addAll(buildSubConditionExpr(subCondition, condition.getLogicalOperator(), dialect));
            }
            ss.add("</trim>");
            return String.join(" ", ss);
        }
        throw new MybatisExtException("Unsupported condition type:" + condition.getType());
    }

    private static String toExpr(Condition condition, Dialect dialect) {
        return toExprWithPrefix(condition, null, dialect);
    }

    private static List<String> buildSubConditionExpr(Condition condition, LogicalOperator logicalOperator, Dialect dialect) {
        List<String> ss = new ArrayList<>();
        if (condition.getType() == ConditionType.COMPLEX && !condition.hasTest() && condition.getLogicalOperator() == logicalOperator && condition.getCollectionVariable() == null && !condition.isNot()) {
            for (Condition subCondition : condition.getSubConditions()) {
                ss.addAll(buildSubConditionExpr(subCondition, logicalOperator, dialect));
            }
        } else {
            ss.add(toScript(condition, logicalOperator, dialect));
        }
        return ss;
    }

    private static String toBasicExpr(Condition condition, CompareOperator compareOperator, boolean ignorecase, Dialect dialect) {
        List<String> ss = new ArrayList<>();
        if (CompareOperator.Equals == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"{variable.##bindName}\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " = {variable.##bindPlaceholder}");
            } else {
                ss.add("{propertyInfo} = {variable.##placeholder}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.LessThan == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"{variable.##bindName}\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &lt; {variable.##bindPlaceholder}");
            } else {
                ss.add("{propertyInfo} &lt; {variable.##placeholder}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.LessThanEqual == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"{variable.##bindName}\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &lt;= {variable.##bindPlaceholder}");
            } else {
                ss.add("{propertyInfo} &lt;= {variable.##placeholder}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.GreaterThan == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"{variable.##bindName}\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &gt; {variable.##bindPlaceholder}");
            } else {
                ss.add("{propertyInfo} &gt; {variable.##placeholder}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.GreaterThanEqual == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"{variable.##bindName}\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &gt;= {variable.##bindPlaceholder}");
            } else {
                ss.add("{propertyInfo} &gt;= {variable.##placeholder}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.Like == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"{variable.##bindName}\" value=\"'%' + " + Ognl.ToUpperCase + "({variable}) + '%'\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " LIKE {variable.##bindPlaceholder}");
            } else {
                ss.add("<bind name=\"{variable.##bindName}\" value=\"'%' + {variable} + '%'\"/>");
                ss.add("{propertyInfo} LIKE {variable.##bindPlaceholder}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.StartWith == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"{variable.##bindName}\" value=\"" + Ognl.ToUpperCase + "({variable}) + '%'\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " LIKE {variable.##bindPlaceholder}");
            } else {
                ss.add("<bind name=\"{variable.##bindName}\" value=\"{variable} + '%'\"/>");
                ss.add("{propertyInfo} LIKE {variable.##bindPlaceholder}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.EndWith == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"{variable.##bindName}\" value=\"'%' + " + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " LIKE {variable.##bindPlaceholder}");
            } else {
                ss.add("<bind name=\"{variable.##bindName}\" value=\"'%' + {variable}\"/>");
                ss.add("{propertyInfo} LIKE {variable.##bindPlaceholder}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.Between == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"{variable.##bindName}\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add("<bind name=\"{secondVariable.##bindName}\" value=\"" + Ognl.ToUpperCase + "({secondVariable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " BETWEEN {variable.##bindPlaceholder} AND {secondVariable.##bindPlaceholder}");
            } else {
                ss.add("{propertyInfo} BETWEEN {variable.##placeholder} AND {secondVariable.##placeholder}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.In == compareOperator) {
            if (ignorecase) {
                ss.add(dialect.upper("{propertyInfo}") + " IN <foreach collection=\"{variable}\" item=\"{variable.##itemName}\" separator=\",\" open=\"(\" close=\")\">");
                ss.add("<bind name=\"{variable.##itemName}\" value=\"" + Ognl.ToUpperCase + "({variable.##itemName})\"/>");
            } else {
                ss.add("{propertyInfo} IN <foreach collection=\"{variable}\" item=\"{variable.##itemName}\" separator=\",\" open=\"(\" close=\")\">");
            }
            ss.add("{variable.##itemPlaceholder}");
            ss.add("</foreach>");
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.IsNull == compareOperator) {
            ss.add("{propertyInfo} IS NULL");
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.IsNotNull == compareOperator) {
            ss.add("{propertyInfo} IS NOT NULL");
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.IsTrue == compareOperator) {
            ss.add("{propertyInfo} " + dialect.isTrue());
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.IsFalse == compareOperator) {
            ss.add("{propertyInfo} " + dialect.isFalse());
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        throw new MybatisExtException("Unsupported compareOperator type:" + compareOperator);
    }
}
