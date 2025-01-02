package io.github.mybatisext.dialect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.jpa.ConditionHelper;
import io.github.mybatisext.jpa.Limit;
import io.github.mybatisext.jpa.OrderByElement;
import io.github.mybatisext.jpa.OrderByType;
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
        String tableAndJoin = buildTableAndJoin(tableInfo, where, null, null);
        String whereSql = where != null ? buildWhere(where) : null;
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, null, null);
        return "<script>" + buildUpdate(
                tableInfo,
                parameter,
                Collection.class.isAssignableFrom(parameter.getJavaType().getType()),
                ignoreNull,
                joinTableInfos.size() > 1,
                tableAndJoin,
                whereSql) + "</script>";
    }

    @Override
    public String insert(TableInfo tableInfo, Variable parameter, boolean ignoreNull) {
        return "<script>" + buildInsert(
                tableInfo,
                parameter,
                Collection.class.isAssignableFrom(parameter.getJavaType().getType()),
                ignoreNull) + "</script>";
    }

    @Override
    public String delete(TableInfo tableInfo, Variable parameter, Condition where) {
        String tableAndJoin = buildTableAndJoin(tableInfo, where, null, null);
        String whereSql = where != null ? buildWhere(where) : null;
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, null, null);
        return "<script>" + buildDelete(
                tableInfo,
                parameter,
                Collection.class.isAssignableFrom(parameter.getJavaType().getType()),
                joinTableInfos.size() > 1,
                tableAndJoin,
                whereSql) + "</script>";
    }

    @Override
    public String select(TableInfo tableInfo, Condition where, boolean distinct, List<OrderByElement> orderBy, List<PropertyInfo> groupBy, Condition having, Limit limit) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT");
        if (groupBy != null) {
            ss.add(buildSelectItems(groupBy));
        } else {
            ss.add(buildSelectItems(tableInfo));
        }
        ss.add("FROM");
        ss.add(buildTableAndJoin(tableInfo, where, groupBy, orderBy));
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
            return "<script>" + buildLimit(limit, String.join(" ", ss)) + "</script>";
        }
        return "<script>" + String.join(" ", ss) + "</script>";
    }

    @Override
    public String exists(TableInfo tableInfo, Condition where) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT 1 FROM");
        ss.add(buildTableAndJoin(tableInfo, where, null, null));
        if (where != null) {
            ss.add(buildWhere(where));
        }
        return "<script>" + buildExists(String.join(" ", ss)) + "</script>";
    }

    @Override
    public String count(TableInfo tableInfo, Condition where) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT COUNT(1) FROM");
        ss.add(buildTableAndJoin(tableInfo, where, null, null));
        if (where != null) {
            ss.add(buildWhere(where));
        }
        return "<script>" + String.join(" ", ss) + "</script>";
    }

    private String buildWhere(Condition condition) {
        return ConditionHelper.toWhere(condition, this);
    }

    private String buildHaving(Condition condition) {
        return ConditionHelper.toHaving(condition, this);
    }

    private String buildTableAndJoin(TableInfo tableInfo, Condition where, List<PropertyInfo> groupBy, List<OrderByElement> orderBy) {
        List<String> ss = new ArrayList<>();
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, groupBy, orderBy);
        ss.add(joinTableInfos.get(0).getTableInfo().getName());
        ss.add("AS");
        ss.add(joinTableInfos.get(0).getAlias());
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            ss.add("LEFT JOIN");
            ss.add(joinTableInfo.getTableInfo().getName());
            ss.add("AS");
            ss.add(joinTableInfo.getAlias());
            ss.add("ON");
            List<String> conditions = new ArrayList<>();
            joinTableInfo.getLeftJoinTableInfos().forEach(((joinColumnInfo, leftJoinTableInfo) -> {
                conditions.add(leftJoinTableInfo.getAlias() + "." + joinColumnInfo.getLeftColumn() + " = " + joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumn());
            }));
            ss.add(String.join(" AND ", conditions));
        }
        return String.join(" ", ss);
    }

    private String buildSelectItems(TableInfo tableInfo) {
        List<String> selectItems = new ArrayList<>();
        tableInfo.getAliasToJoinTableInfo().forEach((alias, joinTableInfo) -> {
            joinTableInfo.getTableInfo().getNameToColumnInfo().forEach((name, columnInfo) -> {
                if (!columnInfo.isReadonly()) {
                    selectItems.add(alias + "." + name + " AS " + alias + "_" + name);
                }
            });
        });
        return String.join(", ", selectItems);
    }

    private String buildSelectItems(List<PropertyInfo> propertyInfos) {
        List<String> selectItems = new ArrayList<>();
        for (PropertyInfo propertyInfo : propertyInfos) {
            if (propertyInfo.getColumnName() != null) {
                if (!propertyInfo.getJoinTableInfo().getTableInfo().getNameToColumnInfo().get(propertyInfo.getColumnName()).isReadonly()) {
                    return propertyInfo.getJoinTableInfo().getAlias() + "." + propertyInfo.getColumnName() + " " + propertyInfo.getJoinTableInfo().getAlias() + "_" + propertyInfo.getColumnName();
                }
            } else {
                selectItems.add(buildSelectItems(new ArrayList<>(propertyInfo.values())));
            }
        }
        return String.join(", ", selectItems);
    }

    private String buildGroupBy(List<PropertyInfo> groupBy) {
        List<String> columns = new ArrayList<>();
        for (PropertyInfo propertyInfo : groupBy) {
            columns.add(propertyInfo.getJoinTableInfo().getAlias() + "." + propertyInfo.getColumnName());
        }
        return "GROUP BY " + String.join(", ", columns);
    }

    private String buildOrderBy(List<OrderByElement> orderBy) {
        List<String> ss = new ArrayList<>();
        for (OrderByElement orderByElement : orderBy) {
            String s = orderByElement.getPropertyInfo().getJoinTableInfo().getAlias() + "." + orderByElement.getPropertyInfo().getColumnName();
            if (OrderByType.ASC == orderByElement.getType()) {
                s += " ASC";
            } else if (OrderByType.DESC == orderByElement.getType()) {
                s += " DESC";
            }
            ss.add(s);
        }
        return "ORDER BY " + String.join(", ", ss);
    }

    private List<JoinTableInfo> collectJoinTableInfo(TableInfo tableInfo, Condition where, List<PropertyInfo> groupBy, List<OrderByElement> orderBy) {
        Set<String> directAliases = new HashSet<>();
        directAliases.add(tableInfo.getJoinTableInfo().getAlias());
        if (where != null) {
            ConditionHelper.collectUsedTableAliases(where, directAliases);
        }
        if (groupBy != null) {
            for (PropertyInfo propertyInfo : groupBy) {
                directAliases.add(propertyInfo.getJoinTableInfo().getAlias());
            }
        }
        if (orderBy != null) {
            for (OrderByElement orderByElement : orderBy) {
                directAliases.add(orderByElement.getPropertyInfo().getJoinTableInfo().getAlias());
            }
        }
        LinkedHashSet<String> orderAliases = new LinkedHashSet<>();
        for (String alias : directAliases) {
            tableInfo.getAliasToJoinTableInfo().get(alias).collectTableAliases(orderAliases);
        }
        return orderAliases.stream().map(v -> tableInfo.getAliasToJoinTableInfo().get(v)).collect(Collectors.toList());
    }
}
