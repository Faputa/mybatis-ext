package io.github.mybatisext.dialect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.jpa.ConditionHelper;
import io.github.mybatisext.jpa.OrderByElement;
import io.github.mybatisext.jpa.OrderByType;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.JoinTableInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.util.StringUtils;

public abstract class BaseDialect implements Dialect {

    protected String buildSimpleInsert(TableInfo tableInfo, Variable variable, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        ss.add("INSERT INTO " + tableInfo.getName());
        ss.add(buildInsertItems(tableInfo, variable, ignoreNull));
        ss.add("VALUES");
        ss.add(buildInsertValues(tableInfo, variable, ignoreNull));
        return String.join(" ", ss);
    }

    protected String buildSimpleUpdate(TableInfo tableInfo, Variable variable, boolean ignoreNull, String where) {
        List<String> ss = new ArrayList<>();
        ss.add("UPDATE");
        ss.add(tableInfo.getName());
        ss.add(tableInfo.getJoinTableInfo().getAlias());
        ss.add(buildUpdateSet(tableInfo.getJoinTableInfo().getAlias(), tableInfo, variable, ignoreNull));
        if (StringUtils.isNotBlank(where)) {
            ss.add(where);
        }
        return String.join(" ", ss);
    }

    protected String buildSimpleDelete(TableInfo tableInfo, String where) {
        List<String> ss = new ArrayList<>();
        ss.add("DELETE FROM");
        ss.add(tableInfo.getName());
        ss.add(tableInfo.getJoinTableInfo().getAlias());
        if (StringUtils.isNotBlank(where)) {
            ss.add(where);
        }
        return String.join(" ", ss);
    }

    protected String buildInsertValues(TableInfo tableInfo, Variable variable, boolean ignoreNull) {
        String s;
        if (ignoreNull) {
            s = "<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\" >";
            s += buildInsertValues(tableInfo.getNameToPropertyInfo().values(), variable, true, true);
            s += "</trim>";
        } else {
            s = "(" + buildInsertValues(tableInfo.getNameToPropertyInfo().values(), variable, false, false) + ")";
        }
        return s;
    }

    private String buildInsertValues(Collection<PropertyInfo> propertyInfos, Variable variable, boolean ignoreNull, boolean hasNext) {
        List<String> ss = new ArrayList<>();
        Iterator<PropertyInfo> iterator = propertyInfos.stream().filter(PropertyInfo::isOwnColumn).iterator();
        while (iterator.hasNext()) {
            PropertyInfo propertyInfo = iterator.next();
            Variable subVariable = new Variable(variable.getFullName(), propertyInfo.getName(), propertyInfo.getJavaType());
            if (propertyInfo.getColumnName() != null) {
                String value = "#{" + subVariable + "}";
                if (hasNext || iterator.hasNext()) {
                    value += ",";
                }
                if (ignoreNull) {
                    value = "<if test=\"" + subVariable + " != null\">" + value + "</if>";
                }
                ss.add(value);
            } else {
                ss.add(buildInsertValues(propertyInfo.values(), subVariable, ignoreNull, hasNext || iterator.hasNext()));
            }
        }
        return String.join(" ", ss);
    }

    protected String buildInsertItems(TableInfo tableInfo, Variable variable, boolean ignoreNull) {
        String s;
        if (ignoreNull) {
            s = "<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\" >";
            s += buildInsertItems(tableInfo.getNameToPropertyInfo().values(), variable, true, true);
            s += "</trim>";
        } else {
            s = "(" + buildInsertItems(tableInfo.getNameToPropertyInfo().values(), variable, false, false) + ")";
        }
        return s;
    }

    private String buildInsertItems(Collection<PropertyInfo> propertyInfos, Variable variable, boolean ignoreNull, boolean hasNext) {
        List<String> ss = new ArrayList<>();
        Iterator<PropertyInfo> iterator = propertyInfos.stream().filter(PropertyInfo::isOwnColumn).iterator();
        while (iterator.hasNext()) {
            PropertyInfo propertyInfo = iterator.next();
            Variable subVariable = new Variable(variable.getFullName(), propertyInfo.getName(), propertyInfo.getJavaType());
            if (propertyInfo.getColumnName() != null) {
                String columnName = propertyInfo.getColumnName();
                if (hasNext || iterator.hasNext()) {
                    columnName += ",";
                }
                if (ignoreNull) {
                    columnName = "<if test=\"" + subVariable + " != null\">" + columnName + "</if>";
                }
                ss.add(columnName);
            } else {
                ss.add(buildInsertItems(propertyInfo.values(), subVariable, ignoreNull, hasNext || iterator.hasNext()));
            }
        }
        return String.join(" ", ss);
    }

    protected String buildUpdateSet(String tableAlias, TableInfo tableInfo, Variable variable, boolean ignoreNull) {
        String s;
        if (ignoreNull) {
            s = "<set>";
            s += buildUpdateSet(tableAlias, tableInfo.getNameToPropertyInfo().values(), variable, true, true);
            s += "</set>";
        } else {
            s = "SET ";
            s += buildUpdateSet(tableAlias, tableInfo.getNameToPropertyInfo().values(), variable, false, false);
        }
        return s;
    }

    private String buildUpdateSet(String tableAlias, Collection<PropertyInfo> propertyInfos, Variable variable, boolean ignoreNull, boolean hasNext) {
        List<String> ss = new ArrayList<>();
        Iterator<PropertyInfo> iterator = propertyInfos.stream().filter(PropertyInfo::isOwnColumn).iterator();
        while (iterator.hasNext()) {
            PropertyInfo propertyInfo = iterator.next();
            Variable subVariable = new Variable(variable.getFullName(), propertyInfo.getName(), propertyInfo.getJavaType());
            if (propertyInfo.getColumnName() != null) {
                String updateItem = tableAlias + "." + propertyInfo.getColumnName() + " = #{" + subVariable + "}";
                if (hasNext || iterator.hasNext()) {
                    updateItem += ",";
                }
                if (ignoreNull) {
                    updateItem = "<if test=\"" + subVariable + " != null\">" + updateItem + "</if>";
                }
                ss.add(updateItem);
            } else {
                ss.add(buildUpdateSet(tableAlias, propertyInfo.values(), subVariable, ignoreNull, hasNext || iterator.hasNext()));
            }
        }
        return String.join(" ", ss);
    }

    protected String buildTableAndJoin(TableInfo tableInfo, Condition where, List<PropertyInfo> selectItems, List<PropertyInfo> groupBy, List<OrderByElement> orderBy) {
        List<String> ss = new ArrayList<>();
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, where, selectItems, groupBy, orderBy);
        ss.add(joinTableInfos.get(0).getTableInfo().getName());
        ss.add(joinTableInfos.get(0).getAlias());
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            ss.add("LEFT JOIN");
            ss.add(joinTableInfo.getTableInfo().toString());
            ss.add(joinTableInfo.getAlias());
            ss.add("ON");
            List<String> conditions = new ArrayList<>();
            joinTableInfo.getLeftJoinTableInfos().forEach((joinColumnInfo, leftJoinTableInfo) -> {
                conditions.add(leftJoinTableInfo.getAlias() + "." + joinColumnInfo.getLeftColumn() + " = " + joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumn());
            });
            ss.add(String.join(" AND ", conditions));
        }
        return String.join(" ", ss);
    }

    protected List<JoinTableInfo> collectJoinTableInfo(TableInfo tableInfo, Condition where, List<PropertyInfo> selectItems, List<PropertyInfo> groupBy, List<OrderByElement> orderBy) {
        Set<String> directAliases = new HashSet<>();
        directAliases.add(tableInfo.getJoinTableInfo().getAlias());
        if (selectItems != null) {
            for (PropertyInfo selectItem : selectItems) {
                directAliases.add(selectItem.getJoinTableInfo().getAlias());
            }
        }
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

    protected String buildSelectItems(List<PropertyInfo> propertyInfos) {
        List<String> selectItems = new ArrayList<>();
        for (PropertyInfo propertyInfo : propertyInfos) {
            if (propertyInfo.getColumnName() != null) {
                if (!propertyInfo.getTableInfo().getNameToColumnInfo().get(propertyInfo.getColumnName()).isReadonly()) {
                    return propertyInfo.getJoinTableInfo().getAlias() + "." + propertyInfo.getColumnName() + " " + propertyInfo.getJoinTableInfo().getAlias() + "_" + propertyInfo.getColumnName();
                }
            } else {
                selectItems.add(buildSelectItems(new ArrayList<>(propertyInfo.values())));
            }
        }
        return String.join(", ", selectItems);
    }

    protected String buildGroupBy(List<PropertyInfo> groupBy) {
        List<String> columns = new ArrayList<>();
        for (PropertyInfo propertyInfo : groupBy) {
            columns.add(propertyInfo.getJoinTableInfo().getAlias() + "." + propertyInfo.getColumnName());
        }
        return "GROUP BY " + String.join(", ", columns);
    }

    protected String buildOrderBy(List<OrderByElement> orderBy) {
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

    protected String buildWhere(Condition condition) {
        return ConditionHelper.toWhere(condition, this);
    }

    protected String buildHaving(Condition condition) {
        return ConditionHelper.toHaving(condition, this);
    }
}
