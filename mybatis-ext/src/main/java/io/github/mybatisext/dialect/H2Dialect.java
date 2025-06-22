package io.github.mybatisext.dialect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.jpa.ConditionHelper;
import io.github.mybatisext.jpa.ConditionType;
import io.github.mybatisext.jpa.Limit;
import io.github.mybatisext.jpa.LogicalOperator;
import io.github.mybatisext.jpa.OrderByElement;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.JoinTableInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.util.TypeArgumentResolver;

public class H2Dialect extends BaseDialect {

    @Override
    public String count(TableInfo tableInfo, Condition where) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT COUNT(1) FROM");
        ss.add(buildTableAndJoin(tableInfo, where, null, null, null));
        if (where != null) {
            ss.add(buildWhere(tableInfo, where));
        }
        return String.join(" ", ss);
    }

    @Override
    public String exists(TableInfo tableInfo, Condition where) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT EXISTS (");
        ss.add("SELECT 1 FROM");
        ss.add(buildTableAndJoin(tableInfo, where, null, null, null));
        if (where != null) {
            ss.add(buildWhere(tableInfo, where));
        }
        ss.add(")");
        return String.join(" ", ss);
    }

    @Override
    public String select(TableInfo tableInfo, List<PropertyInfo> selectItems, Condition where, boolean distinct, List<OrderByElement> orderBy, List<PropertyInfo> groupBy, Condition having, Limit limit) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT");
        if (groupBy != null) {
            ss.add(buildSelectItems(groupBy, this));
        } else {
            ss.add(buildSelectItems(selectItems, this));
        }
        ss.add("FROM");
        ss.add(buildTableAndJoin(tableInfo, where, selectItems, groupBy, orderBy));
        if (where != null) {
            ss.add(buildWhere(tableInfo, where));
        }
        if (groupBy != null) {
            ss.add(buildGroupBy(groupBy));
            if (having != null) {
                ss.add(buildHaving(tableInfo, having));
            }
        }
        if (orderBy != null) {
            ss.add(buildOrderBy(orderBy));
        }
        if (limit != null) {
            return buildLimit(limit, String.join(" ", ss));
        }
        return String.join(" ", ss);
    }

    @Override
    public String update(TableInfo tableInfo, List<PropertyInfo> selectItems, Variable parameter, Condition where, boolean ignoreNull) {
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, selectItems, null, null);
        return buildUpdate(
                tableInfo,
                selectItems,
                parameter,
                where,
                ignoreNull,
                joinTableInfos,
                Collection.class.isAssignableFrom(parameter.getJavaType().getType()),
                joinTableInfos.size() > 1);
    }

    @Override
    public String delete(TableInfo tableInfo, Variable parameter, Condition where) {
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, null, null, null);
        return buildDelete(
                tableInfo,
                parameter,
                where,
                joinTableInfos,
                parameter != null && Collection.class.isAssignableFrom(parameter.getJavaType().getType()),
                joinTableInfos.size() > 1);
    }

    @Override
    public String insert(TableInfo tableInfo, Variable parameter, boolean ignoreNull) {
        return buildInsert(
                tableInfo,
                parameter,
                Collection.class.isAssignableFrom(parameter.getJavaType().getType()),
                ignoreNull);
    }

    private String buildUpdate(TableInfo tableInfo, List<PropertyInfo> selectItems, Variable parameter, Condition where, boolean ignoreNull, List<JoinTableInfo> joinTableInfos, boolean batch, boolean join) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + parameter.getName() + "__item", TypeArgumentResolver.resolveGenericType(parameter.getJavaType(), Collection.class, 0));
            ss.add("<foreach collection=\"" + parameter + "\" item=\"" + "__" + parameter.getName() + "__item\" open=\"\" close=\"\" separator=\";\">");
            ss.add(buildUpdate(tableInfo, selectItems, itemVariable, where, ignoreNull, joinTableInfos, false, join));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        if (join) {
            ss.add("UPDATE");
            ss.add(tableInfo.getName());
            ss.add(tableInfo.getJoinTableInfo().getAlias());
            ss.add(buildUpdateSet(tableInfo.getJoinTableInfo().getAlias(), selectItems, parameter, ignoreNull));
            ss.add(buildWhereExistsJoin(tableInfo, joinTableInfos, where));
            return String.join(" ", ss);
        }
        return buildSimpleUpdate(tableInfo, selectItems, parameter, ignoreNull, buildWhere(tableInfo, where));
    }

    private String buildDelete(TableInfo tableInfo, Variable parameter, Condition where, List<JoinTableInfo> joinTableInfos, boolean batch, boolean join) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + parameter.getName() + "__item", TypeArgumentResolver.resolveGenericType(parameter.getJavaType(), Collection.class, 0));
            ss.add("<foreach collection=\"" + parameter + "\" item=\"" + "__" + parameter.getName() + "__item\" open=\"\" close=\"\" separator=\";\">");
            ss.add(buildDelete(tableInfo, itemVariable, where, joinTableInfos, false, join));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        if (join) {
            ss.add("DELETE FROM");
            ss.add(tableInfo.getName());
            ss.add(tableInfo.getJoinTableInfo().getAlias());
            ss.add(buildWhereExistsJoin(tableInfo, joinTableInfos, where));
            return String.join(" ", ss);
        }
        return buildSimpleDelete(tableInfo, buildWhere(tableInfo, where));
    }

    private String buildInsert(TableInfo tableInfo, Variable variable, boolean batch, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + variable.getName() + "__item", TypeArgumentResolver.resolveGenericType(variable.getJavaType(), Collection.class, 0));
            if (ignoreNull) {
                ss.add("<foreach collection=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"\" close=\"\" separator=\";\">");
                ss.add(buildSimpleInsert(tableInfo, itemVariable, true));
                ss.add("</foreach>");
                return String.join(" ", ss);
            }
            ss.add("INSERT INTO " + tableInfo.getName());
            ss.add(buildInsertItems(tableInfo, itemVariable, false));
            ss.add("VALUES");
            ss.add("<foreach collection=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"\" close=\"\" separator=\",\">");
            ss.add(buildInsertValues(tableInfo, itemVariable, false));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        return buildSimpleInsert(tableInfo, variable, ignoreNull);
    }

    private String buildLimit(Limit limit, String select) {
        List<String> ss = new ArrayList<>();
        ss.add(select);
        if (limit.getOffset() == null && limit.getOffsetVariable() == null) {
            ss.add("LIMIT");
            ss.add(limit.getRowCount() != null ? limit.getRowCount().toString() : "#{" + limit.getRowCountVariable() + "}");
        } else {
            ss.add("LIMIT");
            ss.add(limit.getOffset() != null ? limit.getOffset().toString() : "#{" + limit.getOffsetVariable() + "}");
            ss.add(",");
            ss.add(limit.getRowCount() != null ? limit.getRowCount().toString() : "#{" + limit.getRowCountVariable() + "}");
        }
        return String.join(" ", ss);
    }

    private String buildWhereExistsJoin(TableInfo tableInfo, List<JoinTableInfo> joinTableInfos, Condition where) {
        List<String> ss = new ArrayList<>();
        ss.add("WHERE EXISTS (");
        ss.add("SELECT 1 FROM");
        List<Condition> conditions = new ArrayList<>();
        List<String> tables = new ArrayList<>();
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            tables.add(joinTableInfo.getTableInfo() + " " + joinTableInfo.getAlias());
            joinTableInfo.getLeftJoinTableInfos().forEach((joinColumnInfo, leftJoinTableInfo) -> {
                Condition condition = new Condition(ConditionType.BASIC);
                condition.setExprTemplate(joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumn().getColumnName() + " = " + leftJoinTableInfo.getAlias() + "." + joinColumnInfo.getLeftColumn().getColumnName());
                conditions.add(condition);
            });
        }
        conditions.add(where);
        ss.add(String.join(", ", tables));
        ss.add(ConditionHelper.toWhere(tableInfo, conditions, LogicalOperator.AND, this));
        ss.add(")");
        return String.join(" ", ss);
    }

    @Override
    public String upper(String expr) {
        return "UPPER(" + expr + ")";
    }

    @Override
    public String isTrue() {
        return "IS TRUE";
    }

    @Override
    public String isFalse() {
        return "IS NOT TRUE";
    }

    @Override
    public String quote(String name) {
        return "\"" + name + "\"";
    }

    @Override
    public String subSelect(String select) {
        return "(" + select + ")";
    }
}
