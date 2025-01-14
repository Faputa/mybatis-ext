package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.FilterSpecInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.ResultType;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.ognl.Ognl;
import io.github.mybatisext.util.SimpleStringTemplate;
import io.github.mybatisext.util.StringUtils;

public class ConditionHelper {

    public static Condition fromTableParameter(TableInfo tableInfo, FilterSpecInfo filterSpecInfo, boolean onlyById, String param) {
        Condition condition = buildTableParameter(tableInfo, filterSpecInfo, onlyById, onlyById, param);
        if (condition.getSubConditions().isEmpty() && onlyById) {
            condition = buildTableParameter(tableInfo, filterSpecInfo, false, true, param);
        }
        return simplifyCondition(condition);
    }

    public static Condition fromConditionList(@Nonnull ConditionList conditionList) {
        List<Condition> andConditions = new ArrayList<>();
        List<Condition> orConditions = new ArrayList<>();
        for (ConditionList list = conditionList; list != null; list = list.getTailList()) {
            andConditions.add(conditionList.getCondition());
            if (conditionList.getLogicalOperator() == LogicalOperator.OR) {
                Condition condition = new Condition(ConditionType.COMPLEX);
                condition.setPropertyInfos(conditionList.getCondition().getPropertyInfos());
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
        return simplifyCondition(condition);
    }

    public static @Nullable Condition simplifyCondition(Condition condition) {
        if (condition.getType() == ConditionType.COMPLEX) {
            if (condition.getSubConditions().size() == 1 && condition.getTest() == IfTest.None) {
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

    private static Condition buildTableParameter(TableInfo tableInfo, FilterSpecInfo filterSpecInfo, boolean onlyById, boolean strictMatch, String param) {
        Condition condition = new Condition(ConditionType.COMPLEX);
        condition.setPropertyInfos(tableInfo.getNameToPropertyInfo());
        condition.setLogicalOperator(LogicalOperator.AND);
        if (strictMatch || filterSpecInfo == null) {
            condition.setTest(IfTest.None);
            condition.setLogicalOperator(LogicalOperator.AND);
        } else {
            condition.setTest(filterSpecInfo.getTest());
            condition.setTestTemplate(filterSpecInfo.getTestTemplate());
            condition.setLogicalOperator(filterSpecInfo.getLogicalOperator());
            condition.setExprTemplate(filterSpecInfo.getExprTemplate());
            condition.setVariable(new Variable(StringUtils.isNotBlank(param) ? param : "param1", tableInfo.getTableClass()));
        }
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            Condition subCondition = buildPropertyInfo(tableInfo, propertyInfo, onlyById, strictMatch, param);
            if (subCondition == null) {
                continue;
            }
            condition.getSubConditions().add(subCondition);
        }
        return condition;
    }

    private static @Nullable Condition buildPropertyInfo(TableInfo tableInfo, PropertyInfo propertyInfo, boolean onlyById, boolean strictMatch, String prefix) {
        if (onlyById && (propertyInfo.getResultType() == ResultType.RESULT || !propertyInfo.isOwnColumn())) {
            return null;
        }
        Condition condition = new Condition(StringUtils.isNotBlank(propertyInfo.getColumnName()) ? ConditionType.BASIC : ConditionType.COMPLEX);
        condition.setPropertyInfos(tableInfo.getNameToPropertyInfo());
        condition.setPropertyInfo(propertyInfo);
        condition.setVariable(new Variable(prefix, propertyInfo.getName(), propertyInfo.getJavaType()));
        if (strictMatch || propertyInfo.getFilterSpecInfo() == null) {
            condition.setTest(propertyInfo.getResultType() == ResultType.COLLECTION ? IfTest.NotEmpty : strictMatch ? IfTest.None : IfTest.NotNull);
            condition.setCompareOperator(CompareOperator.Equals);
            condition.setLogicalOperator(LogicalOperator.AND);
        } else {
            condition.setTest(propertyInfo.getFilterSpecInfo().getTest());
            condition.setTestTemplate(propertyInfo.getFilterSpecInfo().getTestTemplate());
            condition.setCompareOperator(propertyInfo.getFilterSpecInfo().getOperator());
            condition.setLogicalOperator(propertyInfo.getFilterSpecInfo().getLogicalOperator());
            condition.setTestTemplate(propertyInfo.getFilterSpecInfo().getExprTemplate());
            if (condition.getCompareOperator() == CompareOperator.Between) {
                PropertyInfo secondPropertyInfo = TableInfoFactory.deepGet(tableInfo, propertyInfo.getFilterSpecInfo().getSecondVariable());
                if (secondPropertyInfo == null) {
                    throw new MybatisExtException("Second variable '" + propertyInfo.getFilterSpecInfo().getSecondVariable() + "' not found in table '" + tableInfo + "'");
                }
                condition.setSecondVariable(new Variable(prefix, propertyInfo.getFilterSpecInfo().getSecondVariable(), secondPropertyInfo.getJavaType()));
            }
        }
        for (PropertyInfo subPropertyInfo : propertyInfo.values()) {
            Condition subCondition = buildPropertyInfo(tableInfo, subPropertyInfo, onlyById, strictMatch, propertyInfo.getResultType() == ResultType.COLLECTION ? condition.getVariable().getFullName() + "[0]" : condition.getVariable().getFullName());
            if (subCondition == null) {
                continue;
            }
            condition.getSubConditions().add(subCondition);
        }
        return condition;
    }

    public static void collectUsedTableAliases(Condition condition, Set<String> tableAliases) {
        if (StringUtils.isNotBlank(condition.getExprTemplate())) {
            ConditionHook conditionHook = new ConditionHook(condition);
            SimpleStringTemplate.build(condition.getExprTemplate(), conditionHook, false);
            tableAliases.addAll(conditionHook.getUsedTableAliases());
        } else if (condition.getType() == ConditionType.COMPLEX) {
            for (Condition subCondition : condition.getSubConditions()) {
                collectUsedTableAliases(subCondition, tableAliases);
            }
        } else {
            tableAliases.add(condition.getPropertyInfo().getJoinTableInfo().getAlias());
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
            ss.add("<where>");
            for (Condition condition : conditions) {
                ss.add(toScript(condition, logicalOperator, dialect));
            }
            ss.add("</where>");
            return String.join(" ", ss);
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

    private static String toScript(Condition condition, @Nullable LogicalOperator prefix, Dialect dialect) {
        if (condition.hasTest()) {
            return "<if test=\"" + toTestOgnl(condition) + "\">" + toExprWithPrefix(condition, prefix, dialect) + "</if>";
        }
        return toExprWithPrefix(condition, prefix, dialect);
    }

    private static String toTestOgnl(Condition condition) {
        if (StringUtils.isNotBlank(condition.getTestTemplate())) {
            return SimpleStringTemplate.build(condition.getTestTemplate(), condition);
        }
        if (condition.getTest() == IfTest.NotEmpty) {
            return Ognl.IsNotEmpty + "(" + condition.getVariable() + ")";
        }
        if (condition.getTest() == IfTest.NotNull) {
            return condition.getVariable() + " != null";
        }
        return null;
    }

    private static String toExprWithPrefix(Condition condition, @Nullable LogicalOperator prefix, Dialect dialect) {
        if (prefix == null) {
            return toExpr(condition, dialect);
        }
        if (StringUtils.isNotBlank(condition.getExprTemplate())) {
            return prefix + " " + SimpleStringTemplate.build(condition.getExprTemplate(), condition);
        }
        if (condition.getType() == ConditionType.BASIC) {
            return prefix + " " + toBasicExpr(condition, condition.getCompareOperator(), condition.isNot(), condition.isIgnorecase(), dialect);
        }
        if (condition.getType() == ConditionType.COMPLEX) {
            List<String> ss = new ArrayList<>();
            ss.add("<trim prefix=\"" + prefix + " (\" suffix=\")\" prefixOverrides=\"" + condition.getLogicalOperator() + "\" >");
            for (Condition subCondition : condition.getSubConditions()) {
                ss.addAll(buildSubConditionExpr(subCondition, condition.getLogicalOperator(), dialect));
            }
            ss.add("</trim>");
            return String.join(" ", ss);
        }
        throw new MybatisExtException("Unsupported condition type:" + condition.getType());
    }

    private static String toExpr(Condition condition, Dialect dialect) {
        if (StringUtils.isNotBlank(condition.getExprTemplate())) {
            return SimpleStringTemplate.build(condition.getExprTemplate(), condition);
        }
        if (condition.getType() == ConditionType.BASIC) {
            return toBasicExpr(condition, condition.getCompareOperator(), condition.isNot(), condition.isIgnorecase(), dialect);
        }
        if (condition.getType() == ConditionType.COMPLEX) {
            List<String> ss = new ArrayList<>();
            ss.add("<trim prefix=\"(\" suffix=\")\" prefixOverrides=\"" + condition.getLogicalOperator() + "\" >");
            for (Condition subCondition : condition.getSubConditions()) {
                ss.addAll(buildSubConditionExpr(subCondition, condition.getLogicalOperator(), dialect));
            }
            ss.add("</trim>");
            return String.join(" ", ss);
        }
        throw new MybatisExtException("Unsupported condition type:" + condition.getType());
    }

    private static List<String> buildSubConditionExpr(Condition condition, LogicalOperator logicalOperator, Dialect dialect) {
        List<String> ss = new ArrayList<>();
        if (condition.getType() == ConditionType.COMPLEX && !condition.hasTest() && condition.getLogicalOperator() == logicalOperator) {
            for (Condition subCondition : condition.getSubConditions()) {
                ss.addAll(buildSubConditionExpr(subCondition, logicalOperator, dialect));
            }
        } else {
            ss.add(toScript(condition, logicalOperator, dialect));
        }
        return ss;
    }

    private static String toBasicExpr(Condition condition, CompareOperator compareOperator, boolean not, boolean ignorecase, Dialect dialect) {
        List<String> ss = new ArrayList<>();
        if (not) {
            ss.add("NOT");
        }
        if (CompareOperator.Equals == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__" + condition.getVariable().getName() + "__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " = #{__{variable.##name}__bind}");
            } else {
                ss.add("{propertyInfo} = #{{variable}}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.LessThan == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &lt; #{__{variable.##name}__bind}");
            } else {
                ss.add("{propertyInfo} &lt; #{{variable}}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.LessThanEqual == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &lt;= #{__{variable.##name}__bind}");
            } else {
                ss.add("{propertyInfo} &lt;= #{{variable}}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.GreaterThan == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &gt; #{__{variable.##name}__bind}");
            } else {
                ss.add("{propertyInfo} &gt; #{{variable}}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.GreaterThanEqual == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &gt;= #{__{variable.##name}__bind}");
            } else {
                ss.add("{propertyInfo} &gt;= #{{variable}}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.Like == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"'%' + " + Ognl.ToUpperCase + "({variable}) + '%'\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " LIKE #{__{variable.##name}__bind}");
            } else {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"'%' + ${{variable}} + '%'\"/>");
                ss.add("{propertyInfo} LIKE #{__{variable.##name}__bind}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.StartWith == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"" + Ognl.ToUpperCase + "({variable}) + '%'\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " LIKE #{__{variable.##name}__bind}");
            } else {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"${{variable}} + '%'\"/>");
                ss.add("{propertyInfo} LIKE #{__{variable.##name}__bind}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.EndWith == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"'%' + " + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " LIKE #{__{variable.##name}__bind}");
            } else {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"'%' + ${{variable}}\"/>");
                ss.add("{propertyInfo} LIKE #{__{variable.##name}__bind}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.Between == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add("<bind name=\"__{secondVariable.##name}__bind\" value=\"" + Ognl.ToUpperCase + "({secondVariable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " BETWEEN #{__{variable.##name}__bind} AND #{__{secondVariable.##name}__bind}");
            } else {
                ss.add("{propertyInfo} BETWEEN #{{variable}} AND #{{secondVariable}}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.In == compareOperator) {
            if (ignorecase) {
                ss.add(dialect.upper("{propertyInfo}") + " IN <foreach collection=\"{variable}\" item=\"__{variable.##name}__item\" separator=\",\" open=\"(\" close=\")\">");
                ss.add("<bind name=\"__{variable.##name}__item\" value=\"" + Ognl.ToUpperCase + "(__{variable.##name}__item)\"/>");
            } else {
                ss.add("{propertyInfo} IN <foreach collection=\"{variable}\" item=\"__{variable.##name}__item\" separator=\",\" open=\"(\" close=\")\">");
            }
            ss.add("#{__{variable.##name}__item}");
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
