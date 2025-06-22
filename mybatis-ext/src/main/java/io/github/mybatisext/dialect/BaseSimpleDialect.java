package io.github.mybatisext.dialect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.jpa.Limit;
import io.github.mybatisext.jpa.OrderByElement;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.JoinTableInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

public abstract class BaseSimpleDialect extends BaseDialect {

    protected abstract String buildUpdate(TableInfo tableInfo, List<PropertyInfo> selectItems, Variable parameter, String tableAndJoin, String where, boolean batch, boolean join, boolean ignoreNull);

    protected abstract String buildDelete(TableInfo tableInfo, Variable parameter, String tableAndJoin, String where, boolean batch, boolean join);

    protected abstract String buildInsert(TableInfo tableInfo, Variable parameter, boolean batch, boolean ignoreNull);

    protected abstract String buildLimit(Limit limit, String select);

    protected abstract String buildExists(String select);

    @Override
    public String update(TableInfo tableInfo, List<PropertyInfo> selectItems, Variable parameter, Condition where, boolean ignoreNull) {
        String tableAndJoin = buildTableAndJoin(tableInfo, where, selectItems, null, null);
        String whereSql = where != null ? buildWhere(tableInfo, where) : null;
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, null, null, null);
        return buildUpdate(
                tableInfo,
                selectItems,
                parameter,
                tableAndJoin,
                whereSql,
                Collection.class.isAssignableFrom(parameter.getJavaType().getType()),
                joinTableInfos.size() > 1, ignoreNull);
    }

    @Override
    public String delete(TableInfo tableInfo, Variable parameter, Condition where) {
        String tableAndJoin = buildTableAndJoin(tableInfo, where, null, null, null);
        String whereSql = where != null ? buildWhere(tableInfo, where) : null;
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, null, null, null);
        return buildDelete(
                tableInfo,
                parameter,
                tableAndJoin,
                whereSql,
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

    @Override
    public String select(TableInfo tableInfo, List<PropertyInfo> selectItems, Condition where, boolean distinct, List<OrderByElement> orderBy, List<PropertyInfo> groupBy, Condition having, Limit limit) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT");
        if (distinct) {
            ss.add("DISTINCT");
        }
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
    public String exists(TableInfo tableInfo, Condition where) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT 1 FROM");
        ss.add(buildTableAndJoin(tableInfo, where, null, null, null));
        if (where != null) {
            ss.add(buildWhere(tableInfo, where));
        }
        return buildExists(String.join(" ", ss));
    }

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
}
