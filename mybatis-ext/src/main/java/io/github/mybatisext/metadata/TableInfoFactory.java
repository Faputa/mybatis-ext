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
        if (StringUtils.isNotBlank(joinTableInfo.getAlias())) {
            tableInfo.getAliasToJoinTableInfo().put(joinTableInfo.getAlias(), joinTableInfo);
        }
    }

    private static void processProperty(Class<?> tableClass, TableInfo tableInfo) {
        AtomicInteger aliasCount = new AtomicInteger(1);
        Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo = new HashMap<>();

        for (Field field : tableClass.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                ColumnInfo columnInfo = buildColumnInfo(column, field.getName());
                tableInfo.getColumnInfos().add(columnInfo);
                PropertyInfo propertyInfo = buildPropertyInfo(field.getName(), field.getType(), tableInfo, columnInfo);
                tableInfo.getNameToPropertyInfo().put(propertyInfo.getName(), propertyInfo);
                continue;
            }
            JoinRelation[] joinRelations = field.getAnnotationsByType(JoinRelation.class);
            if (joinRelations.length > 0) {
                PropertyInfo propertyInfo = buildPropertyInfo(field.getName(), field.getType(), tableInfo, null);
                tableInfo.getNameToPropertyInfo().put(propertyInfo.getName(), propertyInfo);
                processJoinRelations(tableInfo, joinRelations, field.getGenericType(), propertyInfo, featureToJoinTableInfo, aliasCount);
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
            JoinRelation[] joinRelations = readMethod.getAnnotationsByType(JoinRelation.class);
            if (joinRelations.length > 0) {
                PropertyInfo propertyInfo = buildPropertyInfo(propertyDescriptor.getName(), propertyDescriptor.getPropertyType(), tableInfo, null);
                tableInfo.getNameToPropertyInfo().put(propertyInfo.getName(), propertyInfo);
                processJoinRelations(tableInfo, joinRelations, readMethod.getGenericReturnType(), propertyInfo, featureToJoinTableInfo, aliasCount);
            }
        }

        String alias = tableInfo.getJoinTableInfo().getAlias();
        if (StringUtils.isBlank(alias)) {
            aliasCount.set(0);
            while (tableInfo.getAliasToJoinTableInfo().containsKey(alias = "t" + aliasCount.getAndIncrement())) {
            }
            tableInfo.getJoinTableInfo().setAlias(alias);
            tableInfo.getAliasToJoinTableInfo().put(alias, tableInfo.getJoinTableInfo());
        }
    }

    private static void processJoinRelations(TableInfo rootTableInfo, JoinRelation[] joinRelations, Type propertyType, PropertyInfo propertyInfo, Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo, AtomicInteger aliasCount) {
        Map<String, JoinTableInfo> aliasToJoinTableInfo = new HashMap<>();
        if (StringUtils.isNotBlank(rootTableInfo.getJoinTableInfo().getAlias())) {
            aliasToJoinTableInfo.put(rootTableInfo.getJoinTableInfo().getAlias(), rootTableInfo.getJoinTableInfo());
        }
        buildJoinTableInfos(rootTableInfo, joinRelations, propertyType, aliasToJoinTableInfo);
        checkJoinTableInfos(aliasToJoinTableInfo);
        mergeJoinTableInfos(rootTableInfo, propertyInfo, featureToJoinTableInfo, aliasCount);
    }

    private static void buildJoinTableInfos(TableInfo rootTableInfo, JoinRelation[] joinRelations, Type propertyType, Map<String, JoinTableInfo> aliasToJoinTableInfo) {
        JoinTableInfo lastJoinTableInfo = null;
        for (JoinRelation joinRelation : joinRelations) {
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
                        rootTableInfo.getJoinTableInfo().getRightJoinColumnInfos().add(joinColumnInfo);
                        joinColumnInfo.setLeftTable(rootTableInfo.getJoinTableInfo());
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

    private static void mergeJoinTableInfos(TableInfo rootTableInfo, PropertyInfo propertyInfo, Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo, AtomicInteger aliasCount) {
        ArrayList<JoinTableInfo> joinTableInfos = new ArrayList<>();
        rootTableInfo.getJoinTableInfo().setMerged(true);
        joinTableInfos.add(rootTableInfo.getJoinTableInfo());

        for (int i = 0; i < joinTableInfos.size(); i++) {
            // 拷贝一次，防止迭代器失效
            ArrayList<JoinColumnInfo> joinColumnInfos = new ArrayList<>(joinTableInfos.get(i).getRightJoinColumnInfos());
            for (JoinColumnInfo joinColumnInfo : joinColumnInfos) {
                JoinTableInfo joinTableInfo = joinColumnInfo.getRightTable();
                if (joinTableInfo.isMerged()) {
                    continue;
                }
                if (!joinTableInfo.getLeftJoinColumnInfos().stream().map(v -> v.getLeftTable().isMerged()).reduce((a, b) -> a && b).get()) {
                    continue;
                }

                Set<JoinColumnFeature> joinColumnFeatures = joinTableInfo.getLeftJoinColumnInfos().stream().map(v -> buildJoinColumnFeature(v)).collect(Collectors.toSet());
                JoinTableInfo existedJoinTableInfo = featureToJoinTableInfo.get(joinColumnFeatures);

                if (existedJoinTableInfo != null) {
                    for (JoinColumnInfo tmpJoinColumnInfo : joinTableInfo.getLeftJoinColumnInfos()) {
                        tmpJoinColumnInfo.getLeftTable().getRightJoinColumnInfos().remove(tmpJoinColumnInfo);
                    }
                    for (JoinColumnInfo tmpJoinColumnInfo : joinTableInfo.getRightJoinColumnInfos()) {
                        tmpJoinColumnInfo.setLeftTable(existedJoinTableInfo);
                    }
                    joinTableInfo.getLeftJoinColumnInfos().clear();
                    joinTableInfo.getRightJoinColumnInfos().clear();
                    propertyInfo.getTableAliases().add(existedJoinTableInfo.getAlias());
                    continue;
                }

                joinTableInfos.add(joinTableInfo);
                featureToJoinTableInfo.put(joinColumnFeatures, joinTableInfo);

                joinTableInfo.setMerged(true);
                String alias = joinTableInfo.getAlias();
                while (StringUtils.isBlank(alias) || rootTableInfo.getAliasToJoinTableInfo().containsKey(alias)) {
                    alias = "t" + aliasCount.getAndIncrement();
                }
                joinTableInfo.setAlias(alias);
                rootTableInfo.getAliasToJoinTableInfo().put(alias, joinTableInfo);
                propertyInfo.getTableAliases().add(alias);
            }
        }
    }

    private static TableInfo resolveTableInfoFromJoinRelation(Type propertyType, JoinRelation joinRelation) {
        if (joinRelation.table() != void.class) {
            return getTableInfo(joinRelation.table());
        }
        return getTableInfo(unwarpPropertyType(propertyType));
    }

    private static Class<?> unwarpPropertyType(Type propertyType) {
        if (propertyType instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) propertyType).getRawType();
            if (rawType instanceof Class) {
                if (((Class<?>) rawType).isAssignableFrom(List.class)) {
                    return TypeArgumentResolver.resolveTypeArgument(propertyType, List.class, 0);
                }
                if (((Class<?>) rawType).isAssignableFrom(Set.class)) {
                    return TypeArgumentResolver.resolveTypeArgument(propertyType, Set.class, 0);
                }
                if (rawType == Optional.class) {
                    return TypeArgumentResolver.resolveTypeArgument(propertyType, Optional.class, 0);
                }
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
        joinColumnFeature.setLeftTable(joinColumnInfo.getLeftTable());
        joinColumnFeature.setRightTable(joinColumnInfo.getRightTable().getTableInfo());
        return joinColumnFeature;
    }
}
