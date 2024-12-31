package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.ognl.Ognl;
import io.github.mybatisext.util.SimpleStringTemplate;
import io.github.mybatisext.util.StringUtils;

public class ConditionHelper {

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
        List<String> ss = new ArrayList<>();
        if (condition.hasTest()) {
            ss.add("<if test=\"" + toTestOgnl(condition) + "\">");
            if (prefix != null) {
                ss.add(prefix.toString());
            }
            ss.add(toExpr(condition, dialect));
            ss.add("</if>");
            return String.join(" ", ss);
        }
        {
            if (prefix != null) {
                ss.add(prefix.toString());
            }
            ss.add(toExpr(condition, dialect));
            return String.join(" ", ss);
        }
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
