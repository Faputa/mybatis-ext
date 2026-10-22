package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    public static @Nullable Condition simplifyCondition(Condition condition) {
        if (condition.getTestMode() == TestMode.False && StringUtils.isBlank(condition.getTestTemplate())) {
            return null;
        }
        if (condition.getType() == ConditionType.COMPLEX) {
            List<Condition> children = new ArrayList<>(condition.getSubConditions());
            condition.getSubConditions().clear();
            for (Condition child : children) {
                Condition simplified = simplifyCondition(child);
                if (simplified != null) {
                    condition.getSubConditions().add(simplified);
                }
            }
            if (condition.getSubConditions().isEmpty() && StringUtils.isBlank(condition.getExprTemplate())) {
                return null;
            }
            if (isTransparentGroup(condition) && condition.getSubConditions().size() == 1) {
                return condition.getSubConditions().iterator().next();
            }
        }
        return condition;
    }

    /** 仅没有附加语义的括号分组可以折叠或展开。 */
    private static boolean isTransparentGroup(Condition condition) {
        return condition.getType() == ConditionType.COMPLEX
                && condition.getTestMode() == TestMode.None
                && StringUtils.isBlank(condition.getTestTemplate())
                && StringUtils.isBlank(condition.getExprTemplate())
                && condition.getCollectionVariable() == null
                && (condition.getPropertyInfo() == null || condition.getPropertyInfo().getFetchType() == null)
                && !condition.isNot();
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
        applyFilterableSettings(condition, filterableInfo);
        if (condition.getCompareOperator().isRequiredSecondVariable()) {
            PropertyInfo secondPropertyInfo = TableInfoFactory.getPropertyInfo(tableInfo, filterableInfo.getSecondVariable());
            condition.setSecondVariable(new Variable(param, filterableInfo.getSecondVariable(), secondPropertyInfo.getJavaType()));
        }
    }

    static void applyFilterableSettings(Condition condition, FilterableInfo filterableInfo) {
        condition.setTestMode(filterableInfo.getTestMode());
        condition.setCompareOperator(filterableInfo.getOperator());
        condition.setLogicalOperator(filterableInfo.getLogicalOperator());
        condition.setIgnorecase(filterableInfo.isIgnorecase());
        condition.setNot(filterableInfo.isNot());
        condition.setTestTemplate(filterableInfo.getTestTemplate());
        condition.setExprTemplate(filterableInfo.getExprTemplate());
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
        return toWhere(Collections.singletonList(condition), LogicalOperator.AND, dialect);
    }

    public static String toWhere(Collection<Condition> conditions, LogicalOperator logicalOperator, Dialect dialect) {
        RenderContext context = new RenderContext("where", areFixedConditions(conditions));
        String script = toScripts(conditions, logicalOperator, dialect, context);
        return context.fixedConditions ? "WHERE " + script : "<where>" + script + "</where>";
    }

    public static String toHaving(Condition condition, Dialect dialect) {
        return toHaving(Collections.singletonList(condition), LogicalOperator.AND, dialect);
    }

    public static String toHaving(Collection<Condition> conditions, LogicalOperator logicalOperator, Dialect dialect) {
        RenderContext context = new RenderContext("having", areFixedConditions(conditions));
        String script = toScripts(conditions, logicalOperator, dialect, context);
        return context.fixedConditions ? "HAVING " + script : "<trim prefix=\"HAVING\" prefixOverrides=\"AND |OR \">" + script + "</trim>";
    }

    private static boolean areFixedConditions(Collection<Condition> conditions) {
        if (conditions.isEmpty()) {
            return false;
        }
        for (Condition condition : conditions) {
            // 自定义表达式不推断其结构，统一保留动态子句包装。
            if (condition.hasTest()
                    || condition.getTestMode() == TestMode.False
                    || condition.getCollectionVariable() != null
                    || StringUtils.isNotBlank(condition.getExprTemplate())) {
                return false;
            }

            if (condition.getType() == ConditionType.COMPLEX
                    && !areFixedConditions(condition.getSubConditions())) {
                return false;
            }
        }
        return true;
    }

    private static String toScripts(Collection<Condition> conditions, LogicalOperator logicalOperator, Dialect dialect, RenderContext context) {
        List<String> scripts = new ArrayList<>();
        for (Condition condition : conditions) {
            if (isTransparentGroup(condition) && condition.getLogicalOperator() == logicalOperator) {
                scripts.add(toScripts(condition.getSubConditions(), logicalOperator, dialect, context));
            } else if (context.fixedConditions) {
                scripts.add(toExprWithPrefix(condition, null, dialect, context));
            } else {
                scripts.add(toScript(condition, logicalOperator, dialect, context));
            }
        }
        return String.join(context.fixedConditions ? " " + logicalOperator + " " : " ", scripts);
    }

    private static String toScript(Condition condition, LogicalOperator logicalOperator, Dialect dialect, RenderContext context) {
        String script = toExprWithPrefix(condition, logicalOperator, dialect, context);
        if (condition.hasTest() || condition.getTestMode() == TestMode.False) {
            return "<if test=\"" + toTestOgnl(condition) + "\">" + script + "</if>";
        }
        return script;
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

    private static String toExprWithPrefix(Condition condition, @Nullable LogicalOperator logicalOperator, Dialect dialect, RenderContext context) {
        String prefix = logicalOperator == null ? "" : logicalOperator + " ";
        if (condition.isNot()) {
            prefix += "NOT ";
        }
        boolean collection = condition.getCollectionVariable() != null;
        String expression;
        if (StringUtils.isNotBlank(condition.getExprTemplate())) {
            expression = SimpleStringTemplate.build(condition.getExprTemplate(), condition);
        } else if (condition.getType() == ConditionType.BASIC) {
            expression = toBasicExpr(condition, dialect, context);
        } else if (condition.getType() == ConditionType.COMPLEX) {
            String children = toScripts(condition.getSubConditions(), condition.getLogicalOperator(), dialect, context);
            expression = context.fixedConditions
                    ? "(" + children + ")"
                    : "<trim prefix=\"(\" suffix=\")\" prefixOverrides=\"AND |OR \">" + children + "</trim>";
        } else {
            throw new MybatisExtException("Unsupported condition type:" + condition.getType());
        }

        if (collection) {
            expression = "<trim prefix=\"(\" suffix=\")\"><foreach collection=\"" + condition.getCollectionVariable() + "\" item=\"" + condition.getVariable()
                    + "\" separator=\"OR\">" + expression + "</foreach></trim>";
        }
        if (condition.getPropertyInfo() != null && condition.getPropertyInfo().getFetchType() != null) {
            if (context.fixedConditions) {
                return prefix + NestedSelectHelper.buildExistSubSelect(condition.getPropertyInfo(), "(" + expression + ")");
            }
            return "<trim prefix=\"" + prefix + NestedSelectHelper.buildExistSubSelect(condition.getPropertyInfo(), "(\" suffix=\")")
                    + "\">" + expression + "</trim>";
        }
        if (!context.fixedConditions && (condition.getType() == ConditionType.COMPLEX || collection)) {
            return "<trim prefix=\"" + prefix + "\">" + expression + "</trim>";
        }
        return prefix + expression;
    }

    private static String toBasicExpr(Condition condition, Dialect dialect, RenderContext context) {
        CompareOperator operator = condition.getCompareOperator();
        boolean ignorecase = condition.isIgnorecase();
        String property = SimpleStringTemplate.build("{propertyInfo}", condition);
        String column = ignorecase ? dialect.upper(property) : property;
        String sqlOperator = null;
        if (operator == CompareOperator.Equals) {
            sqlOperator = "=";
        } else if (operator == CompareOperator.LessThan) {
            sqlOperator = "&lt;";
        } else if (operator == CompareOperator.LessThanEqual) {
            sqlOperator = "&lt;=";
        } else if (operator == CompareOperator.GreaterThan) {
            sqlOperator = "&gt;";
        } else if (operator == CompareOperator.GreaterThanEqual) {
            sqlOperator = "&gt;=";
        }

        List<String> before = new ArrayList<>();
        Variable variable = condition.getVariable();
        String expression;
        if (sqlOperator != null) {
            String value = ignorecase ? context.bind(variable, Ognl.ToUpperCase + "(" + variable + ")", before) : variable.getPlaceholder();
            expression = column + " " + sqlOperator + " " + value;
        } else if (operator == CompareOperator.Like || operator == CompareOperator.StartWith || operator == CompareOperator.EndWith) {
            String value = ignorecase ? Ognl.ToUpperCase + "(" + variable + ")" : variable.toString();
            if (operator == CompareOperator.Like || operator == CompareOperator.EndWith) {
                value = "'%' + " + value;
            }
            if (operator == CompareOperator.Like || operator == CompareOperator.StartWith) {
                value += " + '%'";
            }
            expression = column + " LIKE " + context.bind(variable, value, before);
        } else if (operator == CompareOperator.Between) {
            Variable second = condition.getSecondVariable();
            String firstValue = ignorecase ? context.bind(variable, Ognl.ToUpperCase + "(" + variable + ")", before) : variable.getPlaceholder();
            String secondValue = ignorecase ? context.bind(second, Ognl.ToUpperCase + "(" + second + ")", before) : second.getPlaceholder();
            expression = column + " BETWEEN " + firstValue + " AND " + secondValue;
        } else if (operator == CompareOperator.In) {
            String item = variable.getItemName();
            String binding = ignorecase ? "<bind name=\"" + item + "\" value=\"" + Ognl.ToUpperCase + "(" + item + ")\"/>" : "";
            expression = column + " IN <foreach collection=\"" + variable + "\" item=\"" + item
                    + "\" separator=\",\" open=\"(\" close=\")\">" + binding + variable.getItemPlaceholder() + "</foreach>";
        } else if (operator == CompareOperator.IsNull) {
            expression = property + " IS NULL";
        } else if (operator == CompareOperator.IsNotNull) {
            expression = property + " IS NOT NULL";
        } else if (operator == CompareOperator.IsTrue) {
            expression = property + " " + dialect.isTrue();
        } else if (operator == CompareOperator.IsFalse) {
            expression = property + " " + dialect.isFalse();
        } else {
            throw new MybatisExtException("Unsupported compareOperator type:" + operator);
        }
        return before.isEmpty() ? expression : String.join(" ", before) + " " + expression;
    }

    /** 绑定名在一个子句内唯一，WHERE 与 HAVING 使用不同命名空间。 */
    private static final class RenderContext {

        private final String clause;
        private final boolean fixedConditions;
        private int bindingIndex;

        private RenderContext(String clause, boolean fixedConditions) {
            this.clause = clause;
            this.fixedConditions = fixedConditions;
        }

        private String bind(Variable source, String expression, List<String> before) {
            String name = "__" + clause + "_bind_" + bindingIndex++;
            before.add("<bind name=\"" + name + "\" value=\"" + expression + "\"/>");
            Variable binding = new Variable(name, source.getJavaType());
            binding.setJdbcType(source.getJdbcType());
            return binding.getPlaceholder();
        }
    }
}
