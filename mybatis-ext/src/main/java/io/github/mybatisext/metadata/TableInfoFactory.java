package io.github.mybatisext.metadata;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.JoinRelations;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.util.StringUtils;
import io.github.mybatisext.util.TypeArgumentResolver;

public class TableInfoFactory {

    private static final Map<Class<?>, TableInfo> tableInfoCache = new ConcurrentHashMap<>();

    public static TableInfo getTableInfo(Class<?> tableClass) {
        if (tableInfoCache.containsKey(tableClass)) {
            return tableInfoCache.get(tableClass);
        }
        TableInfo tableInfo = new TableInfo();
        tableInfoCache.put(tableClass, tableInfo);
        processTable(tableClass, tableInfo);
        processProperty(tableClass, tableInfo);
        return tableInfo;
    }

    private static void processTable(Class<?> tableClass, TableInfo tableInfo) {
        JoinTableInfo joinTableInfo = new JoinTableInfo();
        joinTableInfo.setTableInfo(tableInfo);
        tableInfo.setJoinTableInfo(joinTableInfo);

        Table table = tableClass.getAnnotation(Table.class);
        if (table != null) {
            tableInfo.setName(table.name());
            tableInfo.setComment(table.comment());
            tableInfo.setSchema(table.schema());
            joinTableInfo.setAlias(table.alias());
        }

        if (StringUtils.isBlank(tableInfo.getName())) {
            tableInfo.setName(StringUtils.camelToSnake(tableClass.getSimpleName()));
        }
        if (StringUtils.isBlank(joinTableInfo.getAlias())) {
            tableInfo.getAliasToJoinTableInfo().put("t0", joinTableInfo);
        } else {
            tableInfo.getAliasToJoinTableInfo().put(joinTableInfo.getAlias(), joinTableInfo);
        }
    }

    private static void processProperty(Class<?> tableClass, TableInfo tableInfo) {
        AtomicInteger aliasCount = new AtomicInteger(1);

        for (Field field : tableClass.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                ColumnInfo columnInfo = buildColumnInfo(column, field.getName());
                tableInfo.getColumnInfos().add(columnInfo);
                PropertyInfo propertyInfo = buildPropertyInfo(field.getName(), field.getType(), tableInfo, columnInfo);
                tableInfo.getNameToPropertyInfo().put(propertyInfo.getName(), propertyInfo);
                continue;
            }
            JoinRelations joinRelations = field.getAnnotation(JoinRelations.class);
            if (joinRelations != null) {
                PropertyInfo propertyInfo = buildPropertyInfo(field.getName(), field.getType(), tableInfo, null);
                tableInfo.getNameToPropertyInfo().put(propertyInfo.getName(), propertyInfo);
                processJoinRelations(tableInfo, joinRelations, field.getGenericType(), propertyInfo, aliasCount);
            }
        }

        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(tableClass, Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException e) {
            throw new MybatisExtException(e);
        }

        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            if (tableInfo.getNameToPropertyInfo().containsKey(propertyDescriptor.getName())) {
                continue;
            }
            Method readMethod = propertyDescriptor.getReadMethod();
            if (readMethod == null) {
                continue;
            }
            Column column = readMethod.getAnnotation(Column.class);
            if (column != null) {
                ColumnInfo columnInfo = buildColumnInfo(column, propertyDescriptor.getName());
                tableInfo.getColumnInfos().add(columnInfo);
                PropertyInfo propertyInfo = buildPropertyInfo(propertyDescriptor.getName(), propertyDescriptor.getPropertyType(), tableInfo, columnInfo);
                tableInfo.getNameToPropertyInfo().put(propertyInfo.getName(), propertyInfo);
                continue;
            }
            JoinRelations joinRelations = readMethod.getAnnotation(JoinRelations.class);
            if (joinRelations != null) {
                PropertyInfo propertyInfo = buildPropertyInfo(propertyDescriptor.getName(), propertyDescriptor.getPropertyType(), tableInfo, null);
                tableInfo.getNameToPropertyInfo().put(propertyInfo.getName(), propertyInfo);
                processJoinRelations(tableInfo, joinRelations, readMethod.getGenericReturnType(), propertyInfo, aliasCount);
            }
        }
    }

    private static void processJoinRelations(TableInfo rootTableInfo, JoinRelations joinRelations, Type propertyType, PropertyInfo propertyInfo, AtomicInteger aliasCount) {
        Map<String, JoinTableInfo> aliasToJoinTableInfo = new HashMap<>();
        JoinTableInfo rootJoinTableInfo = rootTableInfo.getJoinTableInfo();
        if (StringUtils.isNotBlank(rootJoinTableInfo.getAlias())) {
            aliasToJoinTableInfo.put(rootJoinTableInfo.getAlias(), rootJoinTableInfo);
        }
        buildJoinTableInfos(joinRelations, propertyType, aliasToJoinTableInfo, rootJoinTableInfo);
        checkJoinTableInfos(aliasToJoinTableInfo);
        mergeJoinTableInfos(rootTableInfo, rootJoinTableInfo, propertyInfo, aliasCount);
    }

    private static void buildJoinTableInfos(JoinRelations joinRelations, Type propertyType, Map<String, JoinTableInfo> aliasToJoinTableInfo, JoinTableInfo rootJoinTableInfo) {
        JoinTableInfo lastJoinTableInfo = null;
        for (JoinRelation joinRelation : joinRelations.value()) {
            JoinTableInfo joinTableInfo;
            TableInfo tableInfo = resolveTableInfoFromJoinRelation(propertyType, joinRelation);
            if (StringUtils.isNotBlank(joinRelation.tableAlias())) {
                joinTableInfo = aliasToJoinTableInfo.get(joinRelation.tableAlias());
                if (joinTableInfo != null) {
                    if (joinTableInfo.getTableInfo() != null) {
                        throw new MybatisExtException("Duplicate table alias: " + joinRelation.tableAlias());
                    }
                    joinTableInfo.setTableInfo(tableInfo);
                } else {
                    joinTableInfo = new JoinTableInfo();
                    joinTableInfo.setTableInfo(tableInfo);
                    aliasToJoinTableInfo.put(joinRelation.tableAlias(), joinTableInfo);
                }
            } else {
                joinTableInfo = new JoinTableInfo();
                joinTableInfo.setTableInfo(tableInfo);
            }

            for (JoinColumn joinColumn : joinRelation.joinColumn()) {
                JoinColumnInfo joinColumnInfo = new JoinColumnInfo();
                joinColumnInfo.setJoinColumn(joinColumn);
                joinColumnInfo.setRightTable(joinTableInfo);
                joinTableInfo.getLeftJoinColumnInfos().add(joinColumnInfo);

                if (StringUtils.isNotBlank(joinColumn.leftTableAlias())) {
                    JoinTableInfo leftJoinTableInfo = aliasToJoinTableInfo.get(joinRelation.tableAlias());
                    if (leftJoinTableInfo != null) {
                        leftJoinTableInfo.getRightJoinColumnInfos().add(joinColumnInfo);
                        joinColumnInfo.setLeftTable(leftJoinTableInfo);
                    } else {
                        leftJoinTableInfo = new JoinTableInfo();
                        leftJoinTableInfo.getRightJoinColumnInfos().add(joinColumnInfo);
                        joinColumnInfo.setLeftTable(leftJoinTableInfo);
                        aliasToJoinTableInfo.put(joinColumn.leftTableAlias(), leftJoinTableInfo);
                    }
                } else {
                    if (lastJoinTableInfo != null) {
                        lastJoinTableInfo.getRightJoinColumnInfos().add(joinColumnInfo);
                        joinColumnInfo.setLeftTable(lastJoinTableInfo);
                    } else {
                        rootJoinTableInfo.getRightJoinColumnInfos().add(joinColumnInfo);
                        joinColumnInfo.setLeftTable(rootJoinTableInfo);
                    }
                }
            }
            lastJoinTableInfo = joinTableInfo;
        }
    }

    private static void checkJoinTableInfos(Map<String, JoinTableInfo> aliasToJoinTableInfo) {
        aliasToJoinTableInfo.forEach((alias, joinTableInfo) -> {
            if (joinTableInfo.getTableInfo() == null) {
                throw new MybatisExtException("Undefined table alias: " + alias);
            }
        });
    }

    private static void mergeJoinTableInfos(TableInfo rootTableInfo, JoinTableInfo rootJoinTableInfo, PropertyInfo propertyInfo, AtomicInteger aliasCount) {
        rootTableInfo.getAliasToJoinTableInfo().forEach((alias, joinTableInfo) -> {
            joinTableInfo.setVisited(false);
        });

        ArrayList<JoinTableInfo> joinTableInfos = new ArrayList<>();
        rootJoinTableInfo.setVisited(true);
        joinTableInfos.add(rootJoinTableInfo);

        int levelBegin = 0;
        int levelEnd = joinTableInfos.size();
        for (int i = levelBegin; i < levelEnd; i++) {
            // 拷贝一次，防止迭代器失效
            ArrayList<JoinColumnInfo> joinColumnInfos = new ArrayList<>(joinTableInfos.get(i).getRightJoinColumnInfos());
            for (JoinColumnInfo joinColumnInfo : joinColumnInfos) {
                JoinTableInfo joinTableInfo = joinColumnInfo.getRightTable();
                if (!joinTableInfo.isMerged()) {
                    continue;
                }
                if (!joinTableInfo.getLeftJoinColumnInfos().stream().map(v -> v.getLeftTable().isVisited()).reduce((a, b) -> a && b).get()) {
                    continue;
                }
                joinTableInfo.setVisited(true);
                joinTableInfos.add(joinTableInfo);

                Set<JoinColumnFeature> joinColumnFeatures = joinTableInfo.getLeftJoinColumnInfos().stream().map(v -> buildJoinColumnFeature(v)).collect(Collectors.toSet());
                for (JoinColumnInfo joinColumnInfo2 : joinTableInfos.get(i).getRightJoinColumnInfos()) {
                    JoinTableInfo joinTableInfo2 = joinColumnInfo2.getRightTable();
                    if (joinTableInfo2.isMerged()) {
                        continue;
                    }
                    if (!joinColumnFeatures.equals(joinTableInfo2.getLeftJoinColumnInfos().stream().map(v -> buildJoinColumnFeature(v)).collect(Collectors.toSet()))) {
                        continue;
                    }
                    for (JoinColumnInfo joinColumnInfo3 : joinTableInfo2.getLeftJoinColumnInfos()) {
                        joinColumnInfo3.getLeftTable().getRightJoinColumnInfos().remove(joinColumnInfo3);
                    }
                    for (JoinColumnInfo joinColumnInfo3 : joinTableInfo2.getRightJoinColumnInfos()) {
                        joinColumnInfo3.setLeftTable(joinTableInfo);
                    }
                    joinTableInfo2.getLeftJoinColumnInfos().clear();
                    joinTableInfo2.getRightJoinColumnInfos().clear();
                    propertyInfo.getTableAliases().add(joinTableInfo.getAlias());
                }
            }

            for (JoinColumnInfo joinColumnInfo : joinTableInfos.get(i).getRightJoinColumnInfos()) {
                JoinTableInfo joinTableInfo = joinColumnInfo.getRightTable();
                if (joinTableInfo.isMerged()) {
                    continue;
                }
                if (!joinTableInfo.getLeftJoinColumnInfos().stream().map(v -> v.getLeftTable().isVisited()).reduce((a, b) -> a && b).get()) {
                    continue;
                }
                joinTableInfo.setVisited(true);
                joinTableInfos.add(joinTableInfo);

                joinTableInfo.setMerged(true);
                String alias = joinTableInfo.getAlias();
                while (StringUtils.isBlank(alias) || rootTableInfo.getAliasToJoinTableInfo().containsKey(alias)) {
                    alias = "t" + aliasCount.getAndIncrement();
                }
                joinTableInfo.setAlias(alias);
                rootTableInfo.getAliasToJoinTableInfo().put(alias, joinTableInfo);
                propertyInfo.getTableAliases().add(alias);
            }

            levelBegin = levelEnd;
            levelEnd = joinTableInfos.size();
        }
    }

    private static TableInfo resolveTableInfoFromJoinRelation(Type propertyType, JoinRelation joinRelation) {
        if (joinRelation.table() != null) {
            return getTableInfo(joinRelation.table());
        }
        return getTableInfo(unwarpPropertyType(propertyType));
    }

    private static Class<?> unwarpPropertyType(Type propertyType) {
        if (propertyType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) propertyType;
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof List) {
                return TypeArgumentResolver.resolveTypeArgument(propertyType, List.class, 0);
            }
            if (rawType instanceof Set) {
                return TypeArgumentResolver.resolveTypeArgument(propertyType, Set.class, 0);
            }
            if (rawType == Optional.class) {
                return TypeArgumentResolver.resolveTypeArgument(propertyType, Optional.class, 0);
            }
        } else if (propertyType instanceof Class) {
            return (Class<?>) propertyType;
        }
        throw new MybatisExtException("Unsupported property type: " + propertyType);
    }

    private static ColumnInfo buildColumnInfo(Column column, String propertyName) {
        ColumnInfo columnInfo = new ColumnInfo();
        columnInfo.setName(StringUtils.isNotBlank(column.name()) ? column.name() : StringUtils.camelToSnake(propertyName));
        columnInfo.setComment(column.comment());
        columnInfo.setLength(column.length());
        columnInfo.setPrecision(column.precision());
        columnInfo.setNullable(column.nullable());
        columnInfo.setScale(column.scale());
        columnInfo.setColumnDefinition(column.columnDefinition());
        return columnInfo;
    }

    private static PropertyInfo buildPropertyInfo(String name, Class<?> propertyClass, TableInfo tableInfo, ColumnInfo columnInfo) {
        PropertyInfo propertyInfo = new PropertyInfo();
        propertyInfo.setName(name);
        propertyInfo.setTableInfo(tableInfo);
        propertyInfo.setColumnInfo(columnInfo);
        if (propertyClass.isAssignableFrom(List.class) || propertyClass.isAssignableFrom(Set.class)) {
            propertyInfo.setCollection(true);
        }
        return propertyInfo;
    }

    private static JoinColumnFeature buildJoinColumnFeature(JoinColumnInfo joinColumnInfo) {
        JoinColumnFeature joinColumnFeature = new JoinColumnFeature();
        joinColumnFeature.setLeftColumn(joinColumnInfo.getJoinColumn().leftColumn());
        joinColumnFeature.setRightColumn(joinColumnInfo.getJoinColumn().rightColumn());
        joinColumnFeature.setLeftTable(joinColumnInfo.getLeftTable().getTableInfo());
        joinColumnFeature.setRightTable(joinColumnInfo.getRightTable().getTableInfo());
        return joinColumnFeature;
    }
}
