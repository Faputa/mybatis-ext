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

    protected abstract String buildUpdate(TableInfo tableInfo, Variable parameter, boolean batch, boolean ignoreNull, boolean join, String tableAndJoin, String where);

    protected abstract String buildInsert(TableInfo tableInfo, Variable parameter, boolean batch, boolean ignoreNull);

    protected abstract String buildDelete(TableInfo tableInfo, Variable parameter, boolean batch, boolean join, String tableAndJoin, String where);

    protected abstract String buildLimit(Limit limit, String select);

    protected abstract String buildExists(String select);

    @Override
    public String update(TableInfo tableInfo, Variable parameter, Condition where, boolean ignoreNull) {
        String tableAndJoin = buildTableAndJoin(tableInfo, where, null, null, null);
        String whereSql = where != null ? buildWhere(where) : null;
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, null, null, null);
        return buildUpdate(
                tableInfo,
                parameter,
                Collection.class.isAssignableFrom(parameter.getJavaType().getType()),
                ignoreNull,
                joinTableInfos.size() > 1,
                tableAndJoin,
                whereSql);
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
    public String delete(TableInfo tableInfo, Variable parameter, Condition where) {
        String tableAndJoin = buildTableAndJoin(tableInfo, where, null, null, null);
        String whereSql = where != null ? buildWhere(where) : null;
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, null, null, null);
        return buildDelete(
                tableInfo,
                parameter,
                Collection.class.isAssignableFrom(parameter.getJavaType().getType()),
                joinTableInfos.size() > 1,
                tableAndJoin,
                whereSql);
    }

    @Override
    public String select(TableInfo tableInfo, Condition where, List<PropertyInfo> selectItems, boolean distinct, List<OrderByElement> orderBy, List<PropertyInfo> groupBy, Condition having, Limit limit) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT");
        if (distinct) {
            ss.add("DISTINCT");
        }
        if (groupBy != null) {
            ss.add(buildSelectItems(tableInfo, groupBy, this));
        } else {
            ss.add(buildSelectItems(tableInfo, selectItems, this));
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
    public String exists(TableInfo tableInfo, Condition where) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT 1 FROM");
        ss.add(buildTableAndJoin(tableInfo, where, null, null, null));
        if (where != null) {
            ss.add(buildWhere(where));
        }
        return buildExists(String.join(" ", ss));
    }

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
}
