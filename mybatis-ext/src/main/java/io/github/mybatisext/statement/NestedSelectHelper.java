package io.github.mybatisext.statement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.JoinColumnInfo;
import io.github.mybatisext.metadata.JoinTableInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

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
        List<JoinColumnInfo> joinColumnInfos = buildLeftmostJoinColumns(nestedSelect.getPropertyInfo());
        for (JoinColumnInfo joinColumnInfo : joinColumnInfos) {
            ss.add(joinColumnInfo.getRightFullName() + "=" + joinColumnInfo.getLeftFullName());
        }
        return "{" + String.join(",", ss) + "}";
    }

    public static List<JoinColumnInfo> buildLeftmostJoinColumns(PropertyInfo propertyInfo) {
        List<JoinColumnInfo> joinColumnInfos = new ArrayList<>();
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(propertyInfo);
        JoinTableInfo rootJoinTableInfo = joinTableInfos.get(0);
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            for (JoinColumnInfo joinColumnInfo : joinTableInfo.getLeftJoinColumnInfos()) {
                if (joinColumnInfo.getLeftJoinTableInfo() == rootJoinTableInfo) {
                    joinColumnInfos.add(joinColumnInfo);
                }
            }
        }
        return joinColumnInfos;
    }

    public static String buildNestedSelectScript(NestedSelect nestedSelect, Dialect dialect) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT");
        ss.add(buildSelectItems(nestedSelect.getPropertyInfo(), dialect));
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(nestedSelect.getPropertyInfo());
        ss.add(buildFrom(joinTableInfos));
        ss.add(buildWhere(joinTableInfos));
        return "<script>" + String.join(" ", ss) + "</script>";
    }

    public static String buildExistSubSelect(PropertyInfo propertyInfo, String nestedCondition) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT 1");
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(propertyInfo);
        ss.add(buildFrom(joinTableInfos));
        ss.add(buildExistWhere(joinTableInfos, nestedCondition));
        return "EXISTS (" + String.join(" ", ss) + ")";
    }

    private static String buildExistWhere(List<JoinTableInfo> joinTableInfos, String nestedCondition) {
        List<String> conditions = new ArrayList<>();
        JoinTableInfo rootJoinTableInfo = joinTableInfos.get(0);
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            for (JoinColumnInfo joinColumnInfo : joinTableInfo.getLeftJoinColumnInfos()) {
                if (joinColumnInfo.getLeftJoinTableInfo() == rootJoinTableInfo) {
                    conditions.add(joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumnName() + " = " + joinColumnInfo.getLeftJoinTableInfo().getAlias() + "." + joinColumnInfo.getLeftColumnName());
                }
            }
        }
        conditions.add(nestedCondition);
        return "WHERE " + String.join(" AND ", conditions);
    }

    private static String buildFrom(List<JoinTableInfo> joinTableInfos) {
        List<String> tables = new ArrayList<>();
        List<String> join = new ArrayList<>();
        join.add("");
        JoinTableInfo rootJoinTableInfo = joinTableInfos.get(0);
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            List<String> conditions = new ArrayList<>();
            for (JoinColumnInfo joinColumnInfo : joinTableInfo.getLeftJoinColumnInfos()) {
                if (joinColumnInfo.getLeftJoinTableInfo() != rootJoinTableInfo) {
                    conditions.add(joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumnName() + " = " + joinColumnInfo.getLeftJoinTableInfo().getAlias() + "." + joinColumnInfo.getLeftColumnName());
                }
            }
            if (conditions.isEmpty()) {
                tables.add(joinTableInfo.getTableName() + " " + joinTableInfo.getAlias());
            } else {
                join.add("LEFT JOIN");
                join.add(joinTableInfo.getTableName().toString());
                join.add(joinTableInfo.getAlias());
                join.add("ON");
                join.add(String.join(" AND ", conditions));
            }
        }
        return "FROM " + String.join(", ", tables) + String.join(" ", join);
    }

    public static String buildWhere(List<JoinTableInfo> joinTableInfos) {
        List<String> conditions = new ArrayList<>();
        JoinTableInfo rootJoinTableInfo = joinTableInfos.get(0);
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            for (JoinColumnInfo joinColumnInfo : joinTableInfo.getLeftJoinColumnInfos()) {
                if (joinColumnInfo.getLeftJoinTableInfo() == rootJoinTableInfo) {
                    conditions.add(joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumnName() + " = #{" + joinColumnInfo.getRightFullName() + "}");
                }
            }
        }
        return "WHERE " + String.join(" AND ", conditions);
    }

    private static List<JoinTableInfo> collectJoinTableInfo(PropertyInfo propertyInfo) {
        HashSet<JoinTableInfo> directJoinTableInfos = new HashSet<>();
        propertyInfo.collectUsedJoinTableInfo(directJoinTableInfos);
        LinkedHashMap<String, JoinTableInfo> orderJoinTableInfos = new LinkedHashMap<>();
        for (JoinTableInfo joinTableInfo : directJoinTableInfos) {
            joinTableInfo.collectJoinTableInfo(orderJoinTableInfos);
        }
        return new ArrayList<>(orderJoinTableInfos.values());
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
        for (PropertyInfo subPropertyInfo : propertyInfo.getNameToPropertyInfo().values()) {
            if (!subPropertyInfo.isReadonly()) {
                selectItems.addAll(buildSelectItemsInner(subPropertyInfo, dialect));
            }
        }
        return selectItems;
    }

    public static String toString(NestedSelect nestedSelect) {
        return NestedSelect.PREFIX + nestedSelect.getTableInfo().getClassType().getName() + "|" + nestedSelect.getPropertyInfo().getName();
    }
}
