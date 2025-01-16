package io.github.mybatisext.statement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.JoinTableInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.util.ImmutablePair;

public class NestedSelectHelper {

    public static NestedSelect buildNestedSelect(TableInfo tableInfo, PropertyInfo propertyInfo) {
        if (propertyInfo.isOwnColumn()) {
            throw new MybatisExtException("Property '" + propertyInfo.getName() + "' is an own column");
        }
        NestedSelect nestedSelect = new NestedSelect();
        nestedSelect.setTableInfo(tableInfo);
        nestedSelect.setPropertyInfo(propertyInfo);
        return nestedSelect;
    }

    public static String buildResultMappingColumn(NestedSelect nestedSelect) {
        List<String> ss = new ArrayList<>();
        List<ImmutablePair<PropertyInfo, PropertyInfo>> immutablePairs = buildLeftmostJoinColumns(nestedSelect.getTableInfo(), nestedSelect.getPropertyInfo());
        for (ImmutablePair<PropertyInfo, PropertyInfo> immutablePair : immutablePairs) {
            ss.add(immutablePair.getRight().getFullName() + "=" + immutablePair.getLeft().getFullName());
        }
        return "{" + String.join(",", ss) + "}";
    }

    public static List<ImmutablePair<PropertyInfo, PropertyInfo>> buildLeftmostJoinColumns(TableInfo tableInfo, PropertyInfo propertyInfo) {
        List<ImmutablePair<PropertyInfo, PropertyInfo>> immutablePairs = new ArrayList<>();
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, propertyInfo);
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            joinTableInfo.getLeftJoinTableInfos().forEach((joinColumnInfo, leftJoinTableInfo) -> {
                if (leftJoinTableInfo.getTableInfo() == tableInfo) {
                    immutablePairs.add(new ImmutablePair<>(joinColumnInfo.getLeftColumn(), joinColumnInfo.getRightColumn()));
                }
            });
        }
        return immutablePairs;
    }

    public static String buildNestedSelectScript(NestedSelect nestedSelect, Dialect dialect) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT");
        ss.add(buildSelectItems(nestedSelect.getPropertyInfo(), dialect));
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(nestedSelect.getTableInfo(), nestedSelect.getPropertyInfo());
        ss.add(buildFrom(joinTableInfos));
        ss.add(buildWhere(nestedSelect.getTableInfo(), joinTableInfos));
        return "<script>" + String.join(" ", ss) + "</script>";
    }

    private static String buildFrom(List<JoinTableInfo> joinTableInfos) {
        List<String> tables = new ArrayList<>();
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            tables.add(joinTableInfo.getTableInfo() + " " + joinTableInfo.getAlias());
        }
        return "FROM " + String.join(", ", tables);
    }

    public static String buildWhere(TableInfo tableInfo, List<JoinTableInfo> joinTableInfos) {
        List<String> conditions = new ArrayList<>();
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            joinTableInfo.getLeftJoinTableInfos().forEach((joinColumnInfo, leftJoinTableInfo) -> {
                if (leftJoinTableInfo.getTableInfo() == tableInfo) {
                    conditions.add("#{" + joinColumnInfo.getRightColumn().getFullName() + "} = " + joinTableInfo.getAlias() + "." + joinColumnInfo.getLeftColumn().getColumnName());
                } else {
                    conditions.add(leftJoinTableInfo.getAlias() + "." + joinColumnInfo.getLeftColumn().getColumnName() + " = " + joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumn().getColumnName());
                }
            });
        }
        return "WHERE " + String.join(" AND ", conditions);
    }

    private static List<JoinTableInfo> collectJoinTableInfo(TableInfo tableInfo, PropertyInfo propertyInfo) {
        LinkedHashSet<String> orderAliases = new LinkedHashSet<>();
        propertyInfo.getJoinTableInfo().collectTableAliases(orderAliases);
        return orderAliases.stream().map(v -> tableInfo.getAliasToJoinTableInfo().get(v)).collect(Collectors.toList());
    }

    private static String buildSelectItems(PropertyInfo propertyInfo, Dialect dialect) {
        List<String> selectItems = new ArrayList<>();
        if (propertyInfo.getColumnName() != null) {
            selectItems.add(propertyInfo.getJoinTableInfo().getAlias() + "." + propertyInfo.getColumnName() + " AS " + dialect.quote(propertyInfo.getFullName()));
        }
        for (PropertyInfo subPropertyInfo : propertyInfo.values()) {
            if (!subPropertyInfo.isReadonly()) {
                selectItems.add(buildSelectItems(subPropertyInfo, dialect));
            }
        }
        return String.join(", ", selectItems);
    }

    public static String toString(NestedSelect nestedSelect) {
        return NestedSelect.PREFIX + nestedSelect.getTableInfo().getTableClass().getName() + "|" + nestedSelect.getPropertyInfo().getName();
    }
}
