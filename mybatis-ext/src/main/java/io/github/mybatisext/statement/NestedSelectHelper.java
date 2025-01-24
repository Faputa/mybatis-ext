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

    public static String buildExistSubSelect(TableInfo tableInfo, PropertyInfo propertyInfo, String nestedCondition, Dialect dialect) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT 1");
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(tableInfo, propertyInfo);
        ss.add(buildFrom(joinTableInfos));
        ss.add(buildExistWhere(tableInfo, joinTableInfos, nestedCondition));
        return "EXISTS " + dialect.subSelect(String.join(" ", ss));
    }

    private static String buildExistWhere(TableInfo tableInfo, List<JoinTableInfo> joinTableInfos, String nestedCondition) {
        List<String> conditions = new ArrayList<>();
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            joinTableInfo.getLeftJoinTableInfos().forEach((joinColumnInfo, leftJoinTableInfo) -> {
                conditions.add(joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumn().getColumnName() + " = " + leftJoinTableInfo.getAlias() + "." + joinColumnInfo.getLeftColumn().getColumnName());
            });
        }
        conditions.add(nestedCondition);
        return "WHERE " + String.join(" AND ", conditions);
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
                    conditions.add(joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumn().getColumnName() + " = #{" + joinColumnInfo.getRightColumn().getFullName() + "}");
                } else {
                    conditions.add(joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumn().getColumnName() + " = " + leftJoinTableInfo.getAlias() + "." + joinColumnInfo.getLeftColumn().getColumnName());
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

    protected static String buildSelectItems(PropertyInfo propertyInfo, Dialect dialect) {
        List<String> selectItemsInner = buildSelectItemsInner(propertyInfo, dialect);
        return String.join(", ", selectItemsInner);
    }

    protected static List<String> buildSelectItemsInner(PropertyInfo propertyInfo, Dialect dialect) {
        List<String> selectItems = new ArrayList<>();
        if (propertyInfo.getColumnName() != null) {
            selectItems.add(propertyInfo.getJoinTableInfo().getAlias() + "." + propertyInfo.getColumnName() + " AS " + dialect.quote(propertyInfo.getFullName()));
        }
        for (PropertyInfo subPropertyInfo : propertyInfo.values()) {
            if (!subPropertyInfo.isReadonly()) {
                selectItems.addAll(buildSelectItemsInner(subPropertyInfo, dialect));
            }
        }
        return selectItems;
    }

    public static String toString(NestedSelect nestedSelect) {
        return NestedSelect.PREFIX + nestedSelect.getTableInfo().getTableClass().getName() + "|" + nestedSelect.getPropertyInfo().getName();
    }
}
