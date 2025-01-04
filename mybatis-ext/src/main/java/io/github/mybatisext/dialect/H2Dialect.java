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

public class H2Dialect extends BaseDialect {

    @Override
    public String count(TableInfo tableInfo, Condition where) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT COUNT(1) FROM");
        ss.add(buildTableAndJoin(tableInfo, where, null, null, null));
        if (where != null) {
            ss.add(buildWhere(where));
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
            ss.add(buildWhere(where));
        }
        ss.add(")");
        return String.join(" ", ss);
    }

    @Override
    public String select(TableInfo tableInfo, Condition where, List<PropertyInfo> selectItems, boolean distinct, List<OrderByElement> orderBy, List<PropertyInfo> groupBy, Condition having, Limit limit) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT");
        if (groupBy != null) {
            ss.add(buildSelectItems(groupBy));
        } else {
            ss.add(buildSelectItems(selectItems));
        }
        ss.add("FROM");
        ss.add(buildTableAndJoin(tableInfo, where, selectItems, groupBy, orderBy));
        if (where != null) {
            ss.add(buildWhere(where));
        }
        if (groupBy != null) {
            ss.add(buildGroupBy(groupBy));
            if (having != null) {
                ss.add(buildHaving(having));
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
    public String delete(TableInfo tableInfo, Variable parameter, Condition where) {
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, null, null, null);
        if (joinTableInfos.size() == 1) {
            return buildSimpleDelete(tableInfo, buildWhere(where));
        }
        List<String> ss = new ArrayList<>();
        ss.add("DELETE FROM");
        ss.add(tableInfo.getName());
        ss.add(tableInfo.getJoinTableInfo().getAlias());
        ss.add(buildWhereExistsJoin(joinTableInfos, where));
        return String.join(" ", ss);
    }

    @Override
    public String insert(TableInfo tableInfo, Variable parameter, boolean ignoreNull) {
        return buildInsert(
                tableInfo,
                parameter,
                Collection.class.isAssignableFrom(parameter.getJavaType().getType()),
                ignoreNull);
    }

    @Override
    public String update(TableInfo tableInfo, Variable parameter, Condition where, boolean ignoreNull) {
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, null, null, null);
        if (joinTableInfos.size() == 1) {
            return buildSimpleUpdate(tableInfo, parameter, ignoreNull, buildWhere(where));
        }
        List<String> ss = new ArrayList<>();
        ss.add("UPDATE");
        ss.add(tableInfo.getName());
        ss.add(tableInfo.getJoinTableInfo().getAlias());
        ss.add(buildUpdateSet(tableInfo.getJoinTableInfo().getAlias(), tableInfo, parameter, ignoreNull));
        ss.add(buildWhereExistsJoin(joinTableInfos, where));
        return String.join(" ", ss);
    }

    private String buildWhereExistsJoin(List<JoinTableInfo> joinTableInfos, Condition where) {
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
                condition.setExprTemplate(leftJoinTableInfo.getAlias() + "." + joinColumnInfo.getLeftColumn() + " = " + joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumn());
                conditions.add(condition);
            });
        }
        conditions.add(where);
        ss.add(String.join(", ", tables));
        ss.add(ConditionHelper.toWhere(conditions, LogicalOperator.AND, this));
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

    private String buildLimit(Limit limit, String select) {
        List<String> ss = new ArrayList<>();
        ss.add(select);
        if (limit.getOffset() == null && limit.getOffsetVariable() != null) {
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

    private String buildInsert(TableInfo tableInfo, Variable variable, boolean batch, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + variable.getName() + "__item", variable.getJavaType().getTypeParameters()[0]);
            if (ignoreNull) {
                ss.add("<foreach Iterable=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"begin\" close=\"; end;\" separator=\";\">");
                ss.add(buildInsert(tableInfo, itemVariable, false, true));
                ss.add("</foreach>");
                return String.join(" ", ss);
            }
            ss.add("INSERT INTO " + tableInfo.getName());
            ss.add(buildInsertItems(tableInfo, itemVariable, false));
            ss.add("VALUES");
            ss.add("<foreach Iterable=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"begin\" close=\"; end;\" separator=\";\">");
            ss.add(buildInsertValues(tableInfo, itemVariable, false));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        return buildSimpleInsert(tableInfo, variable, ignoreNull);
    }
}
