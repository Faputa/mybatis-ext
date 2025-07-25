package io.github.mybatisext.dialect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.jpa.ConditionHelper;
import io.github.mybatisext.jpa.OrderByElement;
import io.github.mybatisext.jpa.OrderByType;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.JoinTableInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.ResultType;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.ognl.Ognl;
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

    protected String buildSimpleUpdate(TableInfo tableInfo, List<PropertyInfo> selectItems, Variable variable, boolean ignoreNull, String where) {
        List<String> ss = new ArrayList<>();
        ss.add("UPDATE");
        ss.add(tableInfo.getName());
        ss.add(tableInfo.getJoinTableInfo().getAlias());
        ss.add(buildUpdateSet(tableInfo.getJoinTableInfo().getAlias(), selectItems, variable, ignoreNull));
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
        Map<PropertyInfo, Variable> map = collectInsertColumns(tableInfo.getNameToPropertyInfo().values(), variable);
        if (ignoreNull) {
            return "<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\" >" + buildInsertValues(map, true) + "</trim>";
        }
        {
            return "(" + buildInsertValues(map, false) + ")";
        }
    }

    private Map<PropertyInfo, Variable> collectInsertColumns(Collection<PropertyInfo> propertyInfos, Variable variable) {
        Map<PropertyInfo, Variable> map = new HashMap<>();
        for (PropertyInfo propertyInfo : propertyInfos) {
            if (!propertyInfo.isOwnColumn()) {
                continue;
            }
            Variable subVariable = new Variable(variable.getFullName(), propertyInfo.getName(), propertyInfo.getJavaType());
            if (propertyInfo.getColumnName() != null) {
                if (propertyInfo.getResultType() != ResultType.ID || propertyInfo.getIdType() != IdType.AUTO) {
                    map.put(propertyInfo, subVariable);
                }
            } else {
                map.putAll(collectInsertColumns(propertyInfo.values(), subVariable));
            }
        }
        return map;
    }

    private String buildInsertValues(Map<PropertyInfo, Variable> map, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        Iterator<Map.Entry<PropertyInfo, Variable>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PropertyInfo, Variable> entry = iterator.next();
            PropertyInfo propertyInfo = entry.getKey();
            Variable variable = entry.getValue();
            String value;
            if (propertyInfo.getResultType() == ResultType.ID && propertyInfo.getIdType() == IdType.UUID) {
                value = "<bind name=\"__" + variable.getName() + "__bind\" value=\"" + Ognl.GetUuid + "('" + variable + "')\"/>#{__" + variable.getName() + "__bind}";
            } else if (propertyInfo.getResultType() == ResultType.ID && propertyInfo.getIdType() == IdType.CUSTOM) {
                value = "<bind name=\"__" + variable.getName() + "__bind\" value=\"" + Ognl.GetUuid + "('" + propertyInfo.getCustomIdGenerator().getName() + "','" + variable + "')\"/>#{__" + variable.getName() + "__bind}";
            } else {
                value = "#{" + variable + "}";
            }
            if (iterator.hasNext()) {
                value += ",";
            }
            if (ignoreNull && propertyInfo.getResultType() != ResultType.ID) {
                value = "<if test=\"" + variable + " != null\">" + value + "</if>";
            }
            ss.add(value);
        }
        return String.join(" ", ss);
    }

    protected String buildInsertItems(TableInfo tableInfo, Variable variable, boolean ignoreNull) {
        Map<PropertyInfo, Variable> map = collectInsertColumns(tableInfo.getNameToPropertyInfo().values(), variable);
        if (ignoreNull) {
            return "<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\" >" + buildInsertItems(map, true) + "</trim>";
        }
        {
            return "(" + buildInsertItems(map, false) + ")";
        }
    }

    private String buildInsertItems(Map<PropertyInfo, Variable> map, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        Iterator<Map.Entry<PropertyInfo, Variable>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PropertyInfo, Variable> entry = iterator.next();
            PropertyInfo propertyInfo = entry.getKey();
            Variable variable = entry.getValue();
            String columnName = propertyInfo.getColumnName();
            if (iterator.hasNext()) {
                columnName += ",";
            }
            if (ignoreNull && propertyInfo.getResultType() != ResultType.ID) {
                columnName = "<if test=\"" + variable + " != null\">" + columnName + "</if>";
            }
            ss.add(columnName);
        }
        return String.join(" ", ss);
    }

    protected String buildUpdateSet(String tableAlias, List<PropertyInfo> selectItems, Variable variable, boolean ignoreNull) {
        Map<PropertyInfo, Variable> map = collectUpdateColumns(selectItems, variable);
        if (ignoreNull) {
            return "<set>" + buildUpdateSet(map, true, tableAlias) + "</set>";
        }
        {
            return "SET " + buildUpdateSet(map, false, tableAlias);
        }
    }

    private Map<PropertyInfo, Variable> collectUpdateColumns(Collection<PropertyInfo> propertyInfos, Variable variable) {
        Map<PropertyInfo, Variable> map = new HashMap<>();
        for (PropertyInfo propertyInfo : propertyInfos) {
            if (!propertyInfo.isOwnColumn()) {
                continue;
            }
            Variable subVariable = new Variable(variable.getFullName(), propertyInfo.getName(), propertyInfo.getJavaType());
            if (propertyInfo.getColumnName() != null) {
                if (propertyInfo.getResultType() != ResultType.ID) {
                    map.put(propertyInfo, subVariable);
                }
            } else {
                map.putAll(collectInsertColumns(propertyInfo.values(), subVariable));
            }
        }
        return map;
    }

    private String buildUpdateSet(Map<PropertyInfo, Variable> map, boolean ignoreNull, String tableAlias) {
        List<String> ss = new ArrayList<>();
        Iterator<Map.Entry<PropertyInfo, Variable>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PropertyInfo, Variable> entry = iterator.next();
            PropertyInfo propertyInfo = entry.getKey();
            Variable variable = entry.getValue();
            String updateItem = tableAlias + "." + propertyInfo.getColumnName() + " = #{" + variable + "}";
            if (iterator.hasNext()) {
                updateItem += ",";
            }
            if (ignoreNull) {
                updateItem = "<if test=\"" + variable + " != null\">" + updateItem + "</if>";
            }
            ss.add(updateItem);
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
                conditions.add(joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumn().getColumnName() + " = " + leftJoinTableInfo.getAlias() + "." + joinColumnInfo.getLeftColumn().getColumnName());
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

    protected String buildSelectItems(Collection<PropertyInfo> propertyInfos, Dialect dialect) {
        List<String> selectItemsInner = buildSelectItemsInner(propertyInfos, dialect);
        return String.join(", ", selectItemsInner);
    }

    protected List<String> buildSelectItemsInner(Collection<PropertyInfo> propertyInfos, Dialect dialect) {
        List<String> selectItems = new ArrayList<>();
        for (PropertyInfo propertyInfo : propertyInfos) {
            if (!propertyInfo.isReadonly()) {
                if (propertyInfo.getColumnName() != null) {
                    selectItems.add(propertyInfo.getJoinTableInfo().getAlias() + "." + propertyInfo.getColumnName() + " AS " + dialect.quote(propertyInfo.getFullName()));
                } else {
                    selectItems.addAll(buildSelectItemsInner(propertyInfo.values(), dialect));
                }
            }
        }
        return selectItems;
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
            if (orderByElement.getType() == OrderByType.ASC) {
                s += " ASC";
            } else if (orderByElement.getType() == OrderByType.DESC) {
                s += " DESC";
            }
            ss.add(s);
        }
        return "ORDER BY " + String.join(", ", ss);
    }

    protected String buildWhere(TableInfo tableInfo, Condition condition) {
        return ConditionHelper.toWhere(tableInfo, condition, this);
    }

    protected String buildHaving(TableInfo tableInfo, Condition condition) {
        return ConditionHelper.toHaving(tableInfo, condition, this);
    }
}
