package io.github.mybatisext.dialect;

import java.util.ArrayList;
import java.util.List;

import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.jpa.Limit;
import io.github.mybatisext.jpa.OrderByElement;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.JoinTableInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

public class SqlServerDialect extends BaseTemplateDialect {

    @Override
    public String select(TableInfo tableInfo, List<PropertyInfo> selectItems, Condition where, boolean distinct, List<OrderByElement> orderBy, List<PropertyInfo> groupBy, Condition having, Limit limit) {
        boolean offset = limit != null && (limit.getOffset() != null || limit.getOffsetVariable() != null);
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, selectItems, groupBy, orderBy);
        List<String> ss = new ArrayList<>();
        ss.add("SELECT");
        if (distinct) {
            ss.add("DISTINCT");
        }
        if (limit != null && !offset) {
            ss.add("TOP (" + limitValue(limit.getRowCount(), limit.getRowCountVariable()) + ")");
        }
        if (groupBy != null) {
            ss.add(buildSelectItems(groupBy, this));
        } else {
            ss.add(buildSelectItems(selectItems, this));
        }
        ss.add("FROM");
        ss.add(buildTableAndJoin(joinTableInfos));
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
        if (offset) {
            if (orderBy == null) {
                ss.add("ORDER BY (SELECT NULL)");
            }
            return buildLimit(limit, String.join(" ", ss));
        }
        return String.join(" ", ss);
    }

    @Override
    protected String buildUpdate(TableInfo tableInfo, List<PropertyInfo> selectItems, Variable parameter, List<JoinTableInfo> joinTableInfos, Condition where, boolean batch, boolean join, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = parameter.getItemVariable();
            ss.add("<foreach collection=\"" + parameter + "\" item=\"" + itemVariable + "\" open=\"\" close=\"\" separator=\";\">");
            ss.add(buildUpdate(tableInfo, selectItems, itemVariable, joinTableInfos, where, false, join, ignoreNull));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        ss.add("UPDATE");
        ss.add(tableInfo.getJoinTableInfo().getAlias());
        ss.add(buildUpdateSet(null, selectItems, parameter, ignoreNull));
        ss.add("FROM");
        ss.add(buildTableAndJoin(joinTableInfos));
        if (where != null) {
            ss.add(buildWhere(where));
        }
        return String.join(" ", ss);
    }

    @Override
    protected String buildDelete(TableInfo tableInfo, Variable parameter, List<JoinTableInfo> joinTableInfos, Condition where, boolean batch, boolean join) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = parameter.getItemVariable();
            ss.add("<foreach collection=\"" + parameter + "\" item=\"" + itemVariable + "\" open=\"\" close=\"\" separator=\";\">");
            ss.add(buildDelete(tableInfo, itemVariable, joinTableInfos, where, false, join));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        ss.add("DELETE");
        ss.add(tableInfo.getJoinTableInfo().getAlias());
        ss.add("FROM");
        ss.add(buildTableAndJoin(joinTableInfos));
        if (where != null) {
            ss.add(buildWhere(where));
        }
        return String.join(" ", ss);
    }

    @Override
    protected String buildInsert(TableInfo tableInfo, Variable variable, boolean batch, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = variable.getItemVariable();
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

    @Override
    protected String buildLimit(Limit limit, String select) {
        List<String> ss = new ArrayList<>();
        ss.add(select);
        ss.add("OFFSET");
        ss.add(limitValue(limit.getOffset(), limit.getOffsetVariable()));
        ss.add("ROWS FETCH NEXT");
        ss.add(limitValue(limit.getRowCount(), limit.getRowCountVariable()));
        ss.add("ROWS ONLY");
        return String.join(" ", ss);
    }

    protected String limitValue(Integer value, Object variable) {
        return value != null ? value.toString() : "#{" + variable + "}";
    }

    @Override
    protected String buildExists(String select) {
        return "SELECT CASE WHEN EXISTS (" + select + ") THEN 1 ELSE 0 END";
    }

    @Override
    public String upper(String expr) {
        return "UPPER(" + expr + ")";
    }

    @Override
    public String isTrue() {
        return "= 1";
    }

    @Override
    public String isFalse() {
        return "= 0";
    }

    @Override
    public String quote(String name) {
        return "[" + name.replace("]", "]]") + "]";
    }
}
