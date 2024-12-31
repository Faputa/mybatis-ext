package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.ognl.Ognl;
import io.github.mybatisext.resultmap.ResultType;
import io.github.mybatisext.util.SimpleStringTemplate;
import io.github.mybatisext.util.StringUtils;

public class ConditionHelper {

    private static final Map<TableInfo, Map<Boolean, Map<IfTest, Map<String, Condition>>>> fromTableInfoCache = new ConcurrentHashMap<>();

    public static Condition fromTableInfo(TableInfo tableInfo, boolean onlyById, IfTest test, String param) {
        Map<Boolean, Map<IfTest, Map<String, Condition>>> map = fromTableInfoCache.computeIfAbsent(tableInfo, k -> new ConcurrentHashMap<>());
        Map<IfTest, Map<String, Condition>> map2 = map.computeIfAbsent(onlyById, k -> new ConcurrentHashMap<>());
        Map<String, Condition> map3 = map2.computeIfAbsent(test, k -> new ConcurrentHashMap<>());
        return map3.computeIfAbsent(param, k -> {
            Condition condition = buildFromTableInfo(tableInfo, onlyById, test, param);
            if (condition.getSubConditions().isEmpty() && onlyById) {
                condition = buildFromTableInfo(tableInfo, false, test, param);
            }
            return simplifyCondition(condition);
        });
    }

    public static Condition fromConditionList(ConditionList conditionList) {
        List<Condition> andConditions = new ArrayList<>();
        List<Condition> orConditions = new ArrayList<>();
        for (; conditionList != null; conditionList = conditionList.getTailList()) {
            andConditions.add(conditionList.getCondition());
            if (conditionList.getLogicalOperator() == LogicalOperator.OR) {
                Condition condition = new Condition(ConditionType.COMPLEX);
                condition.setLogicalOperator(LogicalOperator.AND);
                condition.getSubConditions().addAll(andConditions);
                andConditions.clear();
                orConditions.add(condition);
            }
        }
        if (!andConditions.isEmpty()) {
            Condition condition = new Condition(ConditionType.COMPLEX);
            condition.setLogicalOperator(LogicalOperator.AND);
            condition.getSubConditions().addAll(andConditions);
            orConditions.add(condition);
        }
        Condition condition = new Condition(ConditionType.COMPLEX);
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

    private static Condition buildFromTableInfo(TableInfo tableInfo, boolean onlyById, IfTest test, String param) {
        Condition condition = new Condition(ConditionType.COMPLEX);
        condition.setLogicalOperator(LogicalOperator.AND);
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            Condition subCondition = buildFromPropertyInfo(propertyInfo, onlyById, test, param);
            if (subCondition == null) {
                continue;
            }
            condition.getSubConditions().add(subCondition);
        }
        return condition;
    }

    private static @Nullable Condition buildFromPropertyInfo(PropertyInfo propertyInfo, boolean onlyById, IfTest test, String prefix) {
        if (onlyById && (propertyInfo.getResultType() == ResultType.RESULT || !propertyInfo.isOwnColumn())) {
            return null;
        }
        if (propertyInfo.getResultType() == ResultType.ID || propertyInfo.getResultType() == ResultType.RESULT) {
            Condition condition = new Condition(ConditionType.BASIC);
            condition.setPropertyInfo(propertyInfo);
            condition.setCompareOperator(CompareOperator.Equals);
            condition.setVariable(new Variable(prefix, propertyInfo.getName(), propertyInfo.getJavaType()));
            condition.setTest(test);
            return condition;
        }
        if (propertyInfo.getResultType() == ResultType.ASSOCIATION) {
            Condition condition = new Condition(ConditionType.COMPLEX);
            condition.setLogicalOperator(LogicalOperator.AND);
            condition.setTest(test);
            condition.setVariable(new Variable(prefix, propertyInfo.getName(), propertyInfo.getJavaType()));
            for (PropertyInfo subPropertyInfo : propertyInfo.values()) {
                Condition subCondition = buildFromPropertyInfo(subPropertyInfo, onlyById, test, condition.getVariable().getFullName());
                if (subCondition == null) {
                    continue;
                }
                condition.getSubConditions().add(subCondition);
            }
            return condition;
        }
        if (propertyInfo.getResultType() == ResultType.COLLECTION) {
            Condition condition = new Condition(ConditionType.COMPLEX);
            condition.setLogicalOperator(LogicalOperator.AND);
            condition.setTest(IfTest.NotEmpty);
            condition.setVariable(new Variable(prefix, propertyInfo.getName(), propertyInfo.getJavaType()));
            for (PropertyInfo subPropertyInfo : propertyInfo.values()) {
                Condition subCondition = buildFromPropertyInfo(subPropertyInfo, onlyById, test, condition.getVariable().getFullName() + "[0]");
                if (subCondition == null) {
                    continue;
                }
                condition.getSubConditions().add(subCondition);
            }
            return condition;
        }
        return null;
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

    private static String toScript(Condition condition, LogicalOperator prefix, Dialect dialect) {
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

    private static String toExprWithPrefix(Condition condition, LogicalOperator prefix, Dialect dialect) {
        if (LogicalOperator.AND == prefix) {
            return "AND " + toExpr(condition, dialect);
        }
        if (LogicalOperator.OR == prefix) {
            return "OR " + toExpr(condition, dialect);
        }
        return toExpr(condition, dialect);
    }

    private static String toExpr(Condition condition, Dialect dialect) {
        if (StringUtils.isNotBlank(condition.getExprTemplate())) {
            return SimpleStringTemplate.build(condition.getExprTemplate(), condition);
        }
        if (condition.getType() == ConditionType.BASIC) {
            return toBasicExpr(condition, condition.getCompareOperator(), condition.isNot(), condition.isIgnorecase(), dialect);
        }
        if (condition.getType() == ConditionType.COMPLEX) {
            return toComplexExpr(condition.getSubConditions(), condition.getLogicalOperator(), dialect);
        }
        throw new MybatisExtException("Unsupported condition type:" + condition.getType());
    }

    private static String toComplexExpr(Collection<Condition> conditions, LogicalOperator logicalOperator, Dialect dialect) {
        List<String> ss = new ArrayList<>();
        ss.add("<trim prefix=\"(\" suffix=\")\" prefixOverrides=\"" + logicalOperator + "\" >");
        for (Condition condition : conditions) {
            ss.addAll(buildSubConditionExpr(condition, logicalOperator, dialect));
        }
        ss.add("</trim>");
        return String.join(" ", ss);
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
                ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " = #{__{variable}__bind}");
            } else {
                ss.add("{propertyInfo} = #{{variable}}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.LessThan == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &lt; #{__{variable}__bind}");
            } else {
                ss.add("{propertyInfo} &lt; #{{variable}}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.LessThanEqual == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &lt;= #{__{variable}__bind}");
            } else {
                ss.add("{propertyInfo} &lt;= #{{variable}}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.GreaterThan == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &gt; #{__{variable}__bind}");
            } else {
                ss.add("{propertyInfo} &gt; #{{variable}}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.GreaterThanEqual == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &gt;= #{__{variable}__bind}");
            } else {
                ss.add("{propertyInfo} &gt;= #{{variable}}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.Like == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable}__bind\" value=\"'%' + " + Ognl.ToUpperCase + "({variable}) + '%'\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &gt;= #{__{variable}__bind}");
            } else {
                ss.add("<bind name=\"__{variable}__bind\" value=\"'%' + ${{variable}} + '%'\"/>");
                ss.add("{propertyInfo} &gt;= #{__{variable}__bind}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.StartWith == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable}) + '%'\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &gt;= #{__{variable}__bind}");
            } else {
                ss.add("<bind name=\"__{variable}__bind\" value=\"${{variable}} + '%'\"/>");
                ss.add("{propertyInfo} &gt;= #{__{variable}__bind}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.EndWith == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable}__bind\" value=\"'%' + " + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " &gt;= #{__{variable}__bind}");
            } else {
                ss.add("<bind name=\"__{variable}__bind\" value=\"'%' + ${{variable}}\"/>");
                ss.add("{propertyInfo} &gt;= #{__{variable}__bind}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.Between == compareOperator) {
            if (ignorecase) {
                ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                ss.add("<bind name=\"__{secondVariable}__bind\" value=\"" + Ognl.ToUpperCase + "({secondVariable})\"/>");
                ss.add(dialect.upper("{propertyInfo}") + " BETWEEN #{__{variable}__bind} AND #{__{secondVariable}__bind}");
            } else {
                ss.add("{propertyInfo} BETWEEN #{{variable}} AND #{{secondVariable}}");
            }
            return SimpleStringTemplate.build(String.join(" ", ss), condition);
        }
        if (CompareOperator.In == compareOperator) {
            if (ignorecase) {
                ss.add(dialect.upper("{propertyInfo}") + " IN <foreach collection=\"{variable}\" item=\"__{variable}__item\" separator=\",\" open=\"(\" close=\")\">");
                ss.add("<bind name=\"__{variable}__item\" value=\"" + Ognl.ToUpperCase + "(__{variable}__item)\"/>");
            } else {
                ss.add("{propertyInfo} IN <foreach collection=\"{variable}\" item=\"__{variable}__item\" separator=\",\" open=\"(\" close=\")\">");
            }
            ss.add("#{__{variable}__item}");
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
