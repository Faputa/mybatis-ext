package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.annotation.LoadType;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.FilterableInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.ResultType;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.ognl.Ognl;
import io.github.mybatisext.statement.NestedSelectHelper;
import io.github.mybatisext.util.SimpleStringTemplate;
import io.github.mybatisext.util.StringUtils;

public class ConditionHelper {

    public static Condition fromConditionList(@Nonnull ConditionList conditionList) {
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
        if (condition.getTest() == IfTest.False && StringUtils.isBlank(condition.getTestTemplate())) {
            return null;
        }
        if (condition.getType() == ConditionType.COMPLEX) {
            if (condition.getSubConditions().size() == 1 && condition.getTest() == IfTest.None && StringUtils.isBlank(condition.getTestTemplate())) {
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

    public static Condition fromTableInfo(TableInfo tableInfo, boolean onlyById, String param) {
        Condition condition = buildTableInfo(tableInfo, onlyById, onlyById, param);
        if (condition.getSubConditions().isEmpty() && onlyById) {
            condition = buildTableInfo(tableInfo, false, true, param);
        }
        return condition;
    }

    private static Condition buildTableInfo(TableInfo tableInfo, boolean onlyById, boolean strictMatch, String param) {
        Condition condition = new Condition(ConditionType.COMPLEX);
        condition.setPropertyInfos(tableInfo.getNameToPropertyInfo());
        condition.setLogicalOperator(LogicalOperator.AND);
        condition.setTest(IfTest.None);
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            Condition subCondition = buildPropertyInfo(tableInfo, propertyInfo, onlyById, strictMatch, param, param);
            if (subCondition == null) {
                continue;
            }
            condition.getSubConditions().add(subCondition);
        }
        return condition;
    }

    private static @Nullable Condition buildPropertyInfo(TableInfo tableInfo, PropertyInfo propertyInfo, boolean onlyById, boolean strictMatch, String prefix, String param) {
        if (onlyById && (propertyInfo.getResultType() == ResultType.RESULT || !propertyInfo.isOwnColumn())) {
            return null;
        }
        if (!strictMatch && propertyInfo.getFilterableInfo() == null) {
            return null;
        }
        Condition condition = new Condition(StringUtils.isBlank(propertyInfo.getColumnName()) || propertyInfo.getResultType() == ResultType.COLLECTION ? ConditionType.COMPLEX : ConditionType.BASIC);
        condition.setPropertyInfos(tableInfo.getNameToPropertyInfo());
        condition.setPropertyInfo(propertyInfo);
        condition.setVariable(new Variable(prefix, propertyInfo.getName(), propertyInfo.getJavaType()));
        if (strictMatch) {
            condition.setTest(propertyInfo.getResultType() == ResultType.COLLECTION ? IfTest.NotEmpty : IfTest.None);
            condition.setCompareOperator(CompareOperator.Equals);
            condition.setLogicalOperator(LogicalOperator.AND);
        } else {
            applyFilterableInfo(condition, propertyInfo.getFilterableInfo(), tableInfo, param);
            if (propertyInfo.getResultType() == ResultType.COLLECTION && (condition.getTest() == IfTest.None || condition.getTest() == IfTest.NotNull)) {
                condition.setTest(IfTest.NotEmpty);
            }
        }
        for (PropertyInfo subPropertyInfo : propertyInfo.values()) {
            Condition subCondition = buildPropertyInfo(tableInfo, subPropertyInfo, onlyById, strictMatch, propertyInfo.getResultType() == ResultType.COLLECTION ? condition.getVariable().getFullName() + "[0]" : condition.getVariable().getFullName(), param);
            if (subCondition == null) {
                continue;
            }
            condition.getSubConditions().add(subCondition);
        }
        if (!onlyById && propertyInfo.getResultType() == ResultType.COLLECTION && StringUtils.isNotBlank(propertyInfo.getColumnName())) {
            return splitCollectionResultCondition(condition);
        }
        return condition;
    }

    private static Condition splitCollectionResultCondition(Condition condition) {
        PropertyInfo propertyInfo = condition.getPropertyInfo();
        PropertyInfo collectionPropertyInfo = new PropertyInfo(propertyInfo.getPrefix(), propertyInfo.getName());
        TableInfoFactory.copyPropertyInfoProperties(collectionPropertyInfo, propertyInfo);
        collectionPropertyInfo.setLoadType(null);

        Condition resultCondition = new Condition(ConditionType.BASIC);
        resultCondition.setCompareOperator(condition.getCompareOperator());
        resultCondition.setPropertyInfo(propertyInfo);
        Variable variable = condition.getVariable();
        resultCondition.setVariable(new Variable(variable.getPrefix(), variable.getName() + "[0]", variable.getJavaType()));
        resultCondition.setSecondVariable(condition.getSecondVariable());

        Condition collectionCondition = new Condition(ConditionType.COMPLEX);
        collectionCondition.setTest(condition.getTest());
        collectionCondition.setTestTemplate(condition.getTestTemplate());
        collectionCondition.setVariable(condition.getVariable());
        collectionCondition.setSecondVariable(condition.getSecondVariable());
        collectionCondition.setExprTemplate(condition.getExprTemplate());
        collectionCondition.setIgnorecase(condition.isIgnorecase());
        collectionCondition.setLogicalOperator(LogicalOperator.AND);
        collectionCondition.setNot(condition.isNot());
        collectionCondition.setPropertyInfo(collectionPropertyInfo);
        collectionCondition.setPropertyInfos(condition.getPropertyInfos());
        collectionCondition.getSubConditions().add(resultCondition);
        return collectionCondition;
    }

    private static void applyFilterableInfo(Condition condition, FilterableInfo filterableInfo, TableInfo tableInfo, String param) {
        condition.setTest(filterableInfo.getTest());
        condition.setCompareOperator(filterableInfo.getOperator());
        condition.setLogicalOperator(filterableInfo.getLogicalOperator());
        condition.setIgnorecase(filterableInfo.isIgnorecase());
        condition.setNot(filterableInfo.isNot());
        condition.setTestTemplate(filterableInfo.getTestTemplate());
        condition.setExprTemplate(filterableInfo.getExprTemplate());
        if (condition.getCompareOperator().isRequiredSecondVariable()) {
            PropertyInfo secondPropertyInfo = TableInfoFactory.deepGet(tableInfo, filterableInfo.getSecondVariable());
            if (secondPropertyInfo == null) {
                throw new MybatisExtException("Second variable '" + filterableInfo.getSecondVariable() + "' not found in tableClass '" + tableInfo.getTableClass() + "'");
            }
            condition.setSecondVariable(new Variable(param, filterableInfo.getSecondVariable(), secondPropertyInfo.getJavaType()));
        }
    }

    public static void collectUsedTableAliases(Condition condition, Set<String> tableAliases) {
        if (condition.getPropertyInfo() != null && condition.getPropertyInfo().getLoadType() != null && condition.getPropertyInfo().getLoadType() != LoadType.JOIN) {
            return;
        }
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

    public static String toWhere(TableInfo tableInfo, Condition condition, Dialect dialect) {
        if (condition.hasTest()) {
            return "<where>" + toScript(tableInfo, condition, condition.getLogicalOperator(), dialect) + "</where>";
        }
        if (condition.getType() == ConditionType.COMPLEX) {
            return toWhere(tableInfo, condition.getSubConditions(), condition.getLogicalOperator(), dialect);
        }
        return "WHERE " + toExpr(tableInfo, condition, dialect);
    }

    public static String toWhere(TableInfo tableInfo, Collection<Condition> conditions, LogicalOperator logicalOperator, Dialect dialect) {
        if (conditions.size() == 1) {
            return toWhere(tableInfo, conditions.stream().findFirst().get(), dialect);
        }
        List<String> ss = new ArrayList<>();
        if (conditions.stream().anyMatch(Condition::hasTest)) {
            for (Condition condition : conditions) {
                ss.add(toScript(tableInfo, condition, logicalOperator, dialect));
            }
            return "<where>" + String.join(" ", ss) + "</where>";
        }
        {
            for (Condition condition : conditions) {
                ss.add(toExpr(tableInfo, condition, dialect));
            }
            return "WHERE " + String.join(" " + logicalOperator + " ", ss);
        }
    }

    public static String toHaving(TableInfo tableInfo, Condition condition, Dialect dialect) {
        if (condition.hasTest()) {
            return "<trim prefix=\"HAVING\" prefixOverrides=" + condition.getLogicalOperator() + ">" + toScript(tableInfo, condition, condition.getLogicalOperator(), dialect) + "</trim>";
        }
        if (condition.getType() == ConditionType.COMPLEX) {
            return toHaving(tableInfo, condition.getSubConditions(), condition.getLogicalOperator(), dialect);
        }
        return "HAVING " + toExpr(tableInfo, condition, dialect);
    }

    public static String toHaving(TableInfo tableInfo, Collection<Condition> conditions, LogicalOperator logicalOperator, Dialect dialect) {
        if (conditions.size() == 1) {
            return toHaving(tableInfo, conditions.stream().findFirst().get(), dialect);
        }
        List<String> ss = new ArrayList<>();
        if (conditions.stream().anyMatch(Condition::hasTest)) {
            ss.add("<trim prefix=\"HAVING\" prefixOverrides=" + LogicalOperator.AND + ">");
            for (Condition condition : conditions) {
                ss.add(toScript(tableInfo, condition, LogicalOperator.AND, dialect));
            }
            ss.add("</trim>");
            return String.join(" ", ss);
        }
        {
            for (Condition condition : conditions) {
                ss.add(toExpr(tableInfo, condition, dialect));
            }
            return "HAVING " + String.join(" " + logicalOperator + " ", ss);
        }
    }

    private static String toScript(TableInfo tableInfo, Condition condition, @Nullable LogicalOperator logicalOperator, Dialect dialect) {
        if (condition.hasTest()) {
            if (condition.getSubConditions().size() == 1 && StringUtils.isBlank(condition.getExprTemplate())) {
                Condition subCondition = condition.getSubConditions().iterator().next();
                if (!subCondition.hasTest()) {
                    return "<if test=\"" + toTestOgnl(condition) + "\">" + toExprWithPrefix(tableInfo, subCondition, logicalOperator, dialect) + "</if>";
                }
            }
            return "<if test=\"" + toTestOgnl(condition) + "\">" + toExprWithPrefix(tableInfo, condition, logicalOperator, dialect) + "</if>";
        }
        return toExprWithPrefix(tableInfo, condition, logicalOperator, dialect);
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
        if (condition.getTest() == IfTest.False) {
            return "false";
        }
        return null;
    }

    private static String toExprWithPrefix(TableInfo tableInfo, Condition condition, @Nullable LogicalOperator logicalOperator, Dialect dialect) {
        String prefix = LogicalOperator.AND == logicalOperator ? "AND " : LogicalOperator.OR == logicalOperator ? "OR " : "";
        if (StringUtils.isNotBlank(condition.getExprTemplate())) {
            String expr = SimpleStringTemplate.build(condition.getExprTemplate(), condition);
            if (condition.getPropertyInfo() != null && condition.getPropertyInfo().getLoadType() != null && condition.getPropertyInfo().getLoadType() != LoadType.JOIN) {
                expr = NestedSelectHelper.buildExistSubSelect(tableInfo, condition.getPropertyInfo(), "(" + expr + ")", dialect);
            }
            return prefix + expr;
        }
        if (condition.getType() == ConditionType.BASIC) {
            String expr = toBasicExpr(condition, condition.getCompareOperator(), condition.isNot(), condition.isIgnorecase(), dialect);
            if (condition.getPropertyInfo() != null && condition.getPropertyInfo().getLoadType() != null && condition.getPropertyInfo().getLoadType() != LoadType.JOIN) {
                expr = NestedSelectHelper.buildExistSubSelect(tableInfo, condition.getPropertyInfo(), expr, dialect);
            }
            return prefix + expr;
        }
        if (condition.getType() == ConditionType.COMPLEX) {
            List<String> ss = new ArrayList<>();
            if (condition.getPropertyInfo() != null && condition.getPropertyInfo().getLoadType() != null && condition.getPropertyInfo().getLoadType() != LoadType.JOIN) {
                ss.add("<trim prefix=\"" + prefix + NestedSelectHelper.buildExistSubSelect(tableInfo, condition.getPropertyInfo(), "(\" suffix=\")", dialect) + "\" prefixOverrides=\"" + condition.getLogicalOperator() + "\" >");
            } else {
                ss.add("<trim prefix=\"" + prefix + "(\" suffix=\")\" prefixOverrides=\"" + condition.getLogicalOperator() + "\" >");
            }
            for (Condition subCondition : condition.getSubConditions()) {
                ss.addAll(buildSubConditionExpr(tableInfo, subCondition, condition.getLogicalOperator(), dialect));
            }
            ss.add("</trim>");
            return String.join(" ", ss);
        }
        throw new MybatisExtException("Unsupported condition type:" + condition.getType());
    }

    private static String toExpr(TableInfo tableInfo, Condition condition, Dialect dialect) {
        return toExprWithPrefix(tableInfo, condition, null, dialect);
    }

    private static List<String> buildSubConditionExpr(TableInfo tableInfo, Condition condition, LogicalOperator logicalOperator, Dialect dialect) {
        List<String> ss = new ArrayList<>();
        if (condition.getType() == ConditionType.COMPLEX && !condition.hasTest() && condition.getLogicalOperator() == logicalOperator) {
            for (Condition subCondition : condition.getSubConditions()) {
                ss.addAll(buildSubConditionExpr(tableInfo, subCondition, logicalOperator, dialect));
            }
        } else {
            ss.add(toScript(tableInfo, condition, logicalOperator, dialect));
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
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"'%' + {variable} + '%'\"/>");
                ss.add("{propertyInfo} LIKE #{__{variable.##name}__bind}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.StartWith == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"" + Ognl.ToUpperCase + "({variable}) + '%'\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " LIKE #{__{variable.##name}__bind}");
            } else {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"{variable} + '%'\"/>");
                ss.add("{propertyInfo} LIKE #{__{variable.##name}__bind}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.EndWith == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"'%' + " + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " LIKE #{__{variable.##name}__bind}");
            } else {
                ss.add("<bind name=\"__{variable.##name}__bind\" value=\"'%' + {variable}\"/>");
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
