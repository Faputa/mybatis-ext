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

import javax.annotation.Nullable;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.EmbedParent;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinParent;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.LoadStrategy;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.idgenerator.IdGenerator;
import io.github.mybatisext.resultmap.ResultType;
import io.github.mybatisext.util.StringUtils;
import io.github.mybatisext.util.TypeArgumentResolver;

public class TableInfoFactory {

    private static final Map<Class<?>, TableInfo> tableInfoCache = new ConcurrentHashMap<>();

    // TODO 考虑改为静态方法调用对象方法，缩短参数长度
    public static TableInfo getTableInfo(Class<?> tableClass) {
        if (tableInfoCache.containsKey(tableClass)) {
            return tableInfoCache.get(tableClass);
        }
        TableInfo tableInfo = new TableInfo();
        tableInfoCache.put(tableClass, tableInfo);
        JoinTableInfo joinTableInfo = new JoinTableInfo();
        joinTableInfo.setTableInfo(tableInfo);
        tableInfo.setJoinTableInfo(joinTableInfo);
        tableInfo.setTableClass(tableClass);

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
        processProperty(tableClass, tableInfo);
        return tableInfo;
    }

    private static void processProperty(Class<?> tableClass, TableInfo tableInfo) {
        AtomicInteger aliasCount = new AtomicInteger(1);
        Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo = new HashMap<>();
        Map<String, JoinTableInfo> aliasToJoinTableInfo = new HashMap<>(tableInfo.getAliasToJoinTableInfo());

        List<PropertyInfo> parentPropertyInfos = new ArrayList<>();
        JoinParent joinParent = tableClass.getAnnotation(JoinParent.class);
        if (joinParent != null) {
            processJoinParent(tableClass, joinParent, tableInfo, parentPropertyInfos, featureToJoinTableInfo, aliasToJoinTableInfo);
        }

        for (Class<?> c = tableClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (tableInfo.getNameToPropertyInfo().containsKey(field.getName())) {
                    continue;
                }
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    processColumn(tableInfo, column, field.getAnnotation(Id.class), field.getName(), field.getType());
                } else {
                    JoinRelation[] joinRelations = field.getAnnotationsByType(JoinRelation.class);
                    if (joinRelations.length > 0) {
                        processJoinRelations(tableInfo, joinRelations, field.getAnnotation(LoadStrategy.class), field.getName(), field.getGenericType(), featureToJoinTableInfo, aliasToJoinTableInfo, aliasCount);
                    }
                }
            }
            if (!c.isAnnotationPresent(EmbedParent.class)) {
                break;
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
            if (readMethod.getDeclaringClass() != tableClass && !tableClass.isAnnotationPresent(EmbedParent.class)) {
                continue;
            }
            Column column = readMethod.getAnnotation(Column.class);
            if (column != null) {
                processColumn(tableInfo, column, readMethod.getAnnotation(Id.class), propertyDescriptor.getName(), propertyDescriptor.getPropertyType());
            } else {
                JoinRelation[] joinRelations = readMethod.getAnnotationsByType(JoinRelation.class);
                if (joinRelations.length > 0) {
                    processJoinRelations(tableInfo, joinRelations, readMethod.getAnnotation(LoadStrategy.class), readMethod.getName(), readMethod.getGenericReturnType(), featureToJoinTableInfo, aliasToJoinTableInfo, aliasCount);
                }
            }
        }

        for (PropertyInfo propertyInfo : parentPropertyInfos) {
            if (!tableInfo.getNameToPropertyInfo().containsKey(propertyInfo.getName())) {
                tableInfo.getNameToPropertyInfo().put(propertyInfo.getName(), propertyInfo);
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

    private static void processJoinParent(Class<?> tableClass, JoinParent joinParent, TableInfo tableInfo, List<PropertyInfo> parentPropertyInfos, Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo, Map<String, JoinTableInfo> aliasToJoinTableInfo) {
        Class<?> superclass = tableClass.getSuperclass();
        if (superclass == null || superclass == Object.class) {
            throw new MybatisExtException("Illegal parent class: " + superclass);
        }
        TableInfo parentTableInfo = getTableInfo(superclass);
        JoinTableInfo parentJoinTableInfo = null;

        // 收集别名，拷贝关联关系图
        List<JoinTableInfo> queue = new ArrayList<>();
        Map<JoinTableInfo, JoinTableInfo> oldToNewJoinTableInfo = new HashMap<>();
        queue.add(parentTableInfo.getJoinTableInfo());
        for (int i = 0; i < queue.size(); i++) {
            JoinTableInfo joinTableInfo = queue.get(i);
            JoinTableInfo newJoinTableInfo = new JoinTableInfo();
            newJoinTableInfo.setAlias(joinTableInfo.getAlias());
            newJoinTableInfo.setTableInfo(joinTableInfo.getTableInfo());
            oldToNewJoinTableInfo.put(joinTableInfo, newJoinTableInfo);
            if (parentTableInfo.getJoinTableInfo() == joinTableInfo) {
                parentJoinTableInfo = newJoinTableInfo;
                // 处理关联信息
                for (JoinColumn joinColumn : joinParent.joinColumn()) {
                    JoinColumnInfo joinColumnInfo = new JoinColumnInfo();
                    joinColumnInfo.setLeftColumn(joinColumn.leftColumn());
                    joinColumnInfo.setRightColumn(joinColumn.rightColumn());
                    tableInfo.getJoinTableInfo().getRightJoinTableInfos().put(joinColumnInfo, newJoinTableInfo);
                    newJoinTableInfo.getLeftJoinTableInfos().put(joinColumnInfo, tableInfo.getJoinTableInfo());
                }
                if (StringUtils.isNotBlank(joinParent.alias())) {
                    if (tableInfo.getAliasToJoinTableInfo().containsKey(joinParent.alias())) {
                        throw new MybatisExtException("Duplicate table alias: " + joinParent.alias());
                    }
                    newJoinTableInfo.setAlias(joinParent.alias());
                    aliasToJoinTableInfo.put(joinParent.alias(), newJoinTableInfo);
                }
            }
            joinTableInfo.getRightJoinTableInfos().forEach((joinColumnInfo, rightJoinTable) -> {
                if (!queue.contains(rightJoinTable) && rightJoinTable.getLeftJoinTableInfos().values().stream().map(v -> queue.contains(v)).reduce((a, b) -> a && b).get()) {
                    queue.add(rightJoinTable);
                }
            });
            joinTableInfo.getLeftJoinTableInfos().forEach((joinColumnInfo, leftJoinTable) -> {
                JoinTableInfo newLeftJoinTableInfo = oldToNewJoinTableInfo.get(leftJoinTable);
                assert newLeftJoinTableInfo != null;
                newLeftJoinTableInfo.getRightJoinTableInfos().put(joinColumnInfo, newJoinTableInfo);
                newJoinTableInfo.getLeftJoinTableInfos().put(joinColumnInfo, newLeftJoinTableInfo);
            });
            Set<JoinColumnFeature> joinColumnFeatures = newJoinTableInfo.getLeftJoinTableInfos().entrySet().stream().map(v -> buildJoinColumnFeature(v.getKey(), v.getValue(), newJoinTableInfo)).collect(Collectors.toSet());
            featureToJoinTableInfo.put(joinColumnFeatures, newJoinTableInfo);
            tableInfo.getAliasToJoinTableInfo().put(newJoinTableInfo.getAlias(), newJoinTableInfo);
        }

        // 处理属性信息
        JoinTableInfo finalParentJoinTableInfo = parentJoinTableInfo;
        parentTableInfo.getNameToPropertyInfo().forEach((name, propertyInfo) -> {
            PropertyInfo newPropertyInfo = new PropertyInfo();
            newPropertyInfo.setName(propertyInfo.getName());
            newPropertyInfo.setTableInfo(propertyInfo.getTableInfo());
            newPropertyInfo.setJavaType(propertyInfo.getJavaType());
            newPropertyInfo.setResultType(propertyInfo.getResultType());
            newPropertyInfo.setOfType(propertyInfo.getOfType());
            newPropertyInfo.setLoadType(joinParent.loadType());
            newPropertyInfo.setJoinTableInfo(finalParentJoinTableInfo);
            newPropertyInfo.setColumnName(propertyInfo.getColumnInfo() != null ? propertyInfo.getColumnInfo().getName() : propertyInfo.getColumnName());
            parentPropertyInfos.add(newPropertyInfo);
        });
    }

    private static void processColumn(TableInfo tableInfo, Column column, @Nullable Id id, String propertyName, Class<?> propertyClass) {
        PropertyInfo propertyInfo = new PropertyInfo();
        processIdType(propertyInfo, id);
        propertyInfo.setName(propertyName);
        propertyInfo.setTableInfo(tableInfo);

        ColumnInfo columnInfo = new ColumnInfo();
        columnInfo.setName(StringUtils.isNotBlank(column.name()) ? column.name() : StringUtils.camelToSnake(propertyName));
        columnInfo.setComment(column.comment());
        columnInfo.setNullable(column.nullable());
        columnInfo.setColumnDefinition(column.columnDefinition());
        columnInfo.setLength(column.length());
        columnInfo.setPrecision(column.precision());
        columnInfo.setScale(column.scale());
        columnInfo.setJdbcType(column.jdbcType());
        propertyInfo.setColumnInfo(columnInfo);

        tableInfo.getNameToColumnInfo().put(propertyName, columnInfo);
        tableInfo.getNameToPropertyInfo().put(propertyName, propertyInfo);
    }

    private static void processIdType(PropertyInfo propertyInfo, @Nullable Id id) {
        if (id == null) {
            propertyInfo.setResultType(ResultType.RESULT);
            return;
        }
        if (id.customIdGenerator() == void.class) {
            if (IdType.CUSTOM.equals(id.idType())) {
                throw new MybatisExtException("customIdGenerator cannot be null");
            }
        } else {
            if (!IdGenerator.class.isAssignableFrom(id.customIdGenerator())) {
                throw new MybatisExtException("customIdGenerator must implement the IdGenerator");
            }
            try {
                IdGenerator<?> customIdGenerator = (IdGenerator<?>) id.customIdGenerator().newInstance();
                propertyInfo.setCustomIdGenerator(customIdGenerator);
            } catch (Exception e) {
                throw new MybatisExtException(e);
            }
        }
        propertyInfo.setIdType(id.idType());
        propertyInfo.setResultType(ResultType.ID);
    }

    private static void processJoinRelations(TableInfo tableInfo, JoinRelation[] joinRelations, @Nullable LoadStrategy loadStrategy, String propertyName, Type propertyType, Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo, Map<String, JoinTableInfo> aliasToJoinTableInfo, AtomicInteger aliasCount) {
        PropertyInfo propertyInfo = new PropertyInfo();
        propertyInfo.setName(propertyName);
        propertyInfo.setTableInfo(tableInfo);
        if (loadStrategy != null) {
            propertyInfo.setLoadType(loadStrategy.value());
        }
        resolvePropertyType(propertyInfo, propertyType);
        tableInfo.getNameToPropertyInfo().put(propertyName, propertyInfo);

        JoinRelation lastJoinRelation = joinRelations[joinRelations.length - 1];
        if (lastJoinRelation.table() != void.class) {
            if (StringUtils.isNotBlank(lastJoinRelation.column())) {
                propertyInfo.setColumnName(lastJoinRelation.column());
            } else {
                propertyInfo.setColumnName(StringUtils.camelToSnake(propertyName));
            }
        }

        Map<String, JoinTableInfo> localAliasToJoinTableInfo = new HashMap<>(aliasToJoinTableInfo);
        buildJoinTableInfos(tableInfo, propertyInfo, joinRelations, localAliasToJoinTableInfo);
        checkJoinTableInfos(localAliasToJoinTableInfo);
        mergeJoinTableInfos(tableInfo, propertyInfo, featureToJoinTableInfo, aliasCount);
    }

    private static void buildJoinTableInfos(TableInfo rootTableInfo, PropertyInfo propertyInfo, JoinRelation[] joinRelations, Map<String, JoinTableInfo> aliasToJoinTableInfo) {
        JoinTableInfo lastJoinTableInfo = null;
        for (JoinRelation joinRelation : joinRelations) {
            JoinTableInfo joinTableInfo;
            TableInfo tableInfo = resolveTableInfoFromJoinRelation(propertyInfo, joinRelation);
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
                joinColumnInfo.setLeftColumn(joinColumn.leftColumn());
                joinColumnInfo.setRightColumn(joinColumn.rightColumn());

                if (StringUtils.isNotBlank(joinColumn.leftTableAlias())) {
                    JoinTableInfo leftJoinTableInfo = aliasToJoinTableInfo.get(joinColumn.leftTableAlias());
                    if (leftJoinTableInfo != null) {
                        leftJoinTableInfo.getRightJoinTableInfos().put(joinColumnInfo, joinTableInfo);
                        joinTableInfo.getLeftJoinTableInfos().put(joinColumnInfo, leftJoinTableInfo);
                    } else {
                        leftJoinTableInfo = new JoinTableInfo();
                        leftJoinTableInfo.getRightJoinTableInfos().put(joinColumnInfo, joinTableInfo);
                        joinTableInfo.getLeftJoinTableInfos().put(joinColumnInfo, leftJoinTableInfo);
                        aliasToJoinTableInfo.put(joinColumn.leftTableAlias(), leftJoinTableInfo);
                    }
                } else {
                    if (lastJoinTableInfo != null) {
                        lastJoinTableInfo.getRightJoinTableInfos().put(joinColumnInfo, joinTableInfo);
                        joinTableInfo.getLeftJoinTableInfos().put(joinColumnInfo, lastJoinTableInfo);
                    } else {
                        rootTableInfo.getJoinTableInfo().getRightJoinTableInfos().put(joinColumnInfo, joinTableInfo);
                        joinTableInfo.getLeftJoinTableInfos().put(joinColumnInfo, rootTableInfo.getJoinTableInfo());
                    }
                }
            }
            lastJoinTableInfo = joinTableInfo;
        }
        propertyInfo.setJoinTableInfo(lastJoinTableInfo);
    }

    private static void checkJoinTableInfos(Map<String, JoinTableInfo> aliasToJoinTableInfo) {
        aliasToJoinTableInfo.forEach((alias, joinTableInfo) -> {
            if (joinTableInfo.getTableInfo() == null) {
                throw new MybatisExtException("Undefined table alias: " + alias);
            }
        });
    }

    private static void mergeJoinTableInfos(TableInfo rootTableInfo, PropertyInfo propertyInfo, Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo, AtomicInteger aliasCount) {
        List<JoinTableInfo> queue = new ArrayList<>();
        queue.add(rootTableInfo.getJoinTableInfo());

        for (int i = 0; i < queue.size(); i++) {
            Set<JoinTableInfo> joinTableInfos = queue.get(i).getRightJoinTableInfos().values().stream().collect(Collectors.toSet());
            for (JoinTableInfo joinTableInfo : joinTableInfos) {
                if (!queue.contains(joinTableInfo) && joinTableInfo.getLeftJoinTableInfos().values().stream().map(v -> queue.contains(v)).reduce((a, b) -> a && b).get()) {
                    queue.add(joinTableInfo);
                }
                if (joinTableInfo.isMerged()) {
                    continue;
                }

                Set<JoinColumnFeature> joinColumnFeatures = joinTableInfo.getLeftJoinTableInfos().entrySet().stream().map(v -> buildJoinColumnFeature(v.getKey(), v.getValue(), joinTableInfo)).collect(Collectors.toSet());
                JoinTableInfo existedJoinTableInfo = featureToJoinTableInfo.get(joinColumnFeatures);

                if (existedJoinTableInfo != null) {
                    joinTableInfo.getLeftJoinTableInfos().forEach((leftJoinColumnInfo, leftJoinTableInfo) -> {
                        leftJoinTableInfo.getRightJoinTableInfos().remove(leftJoinColumnInfo);
                    });
                    joinTableInfo.getRightJoinTableInfos().forEach((rightJoinColumnInfo, rightJoinTableInfo) -> {
                        rightJoinTableInfo.getLeftJoinTableInfos().put(rightJoinColumnInfo, existedJoinTableInfo);
                        existedJoinTableInfo.getRightJoinTableInfos().put(rightJoinColumnInfo, rightJoinTableInfo);
                    });
                    joinTableInfo.getLeftJoinTableInfos().clear();
                    joinTableInfo.getRightJoinTableInfos().clear();
                    if (propertyInfo.getJoinTableInfo() == joinTableInfo) {
                        propertyInfo.setJoinTableInfo(existedJoinTableInfo);
                    }
                    continue;
                }

                featureToJoinTableInfo.put(joinColumnFeatures, joinTableInfo);

                String alias = joinTableInfo.getAlias();
                while (StringUtils.isBlank(alias) || rootTableInfo.getAliasToJoinTableInfo().containsKey(alias)) {
                    alias = "t" + aliasCount.getAndIncrement();
                }
                joinTableInfo.setAlias(alias);
                rootTableInfo.getAliasToJoinTableInfo().put(alias, joinTableInfo);
            }
        }
    }

    private static TableInfo resolveTableInfoFromJoinRelation(PropertyInfo propertyInfo, JoinRelation joinRelation) {
        if (joinRelation.table() != void.class) {
            return getTableInfo(joinRelation.table());
        }
        if (propertyInfo.getOfType() != null) {
            return getTableInfo(propertyInfo.getOfType());
        }
        return getTableInfo(propertyInfo.getJavaType());
    }

    private static void resolvePropertyType(PropertyInfo propertyInfo, Type propertyType) {
        if (propertyType instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) propertyType).getRawType();
            if (rawType instanceof Class) {
                if (List.class.isAssignableFrom((Class<?>) rawType)) {
                    propertyInfo.setJavaType(List.class);
                    propertyInfo.setOfType(TypeArgumentResolver.resolveTypeArgument(propertyType, List.class, 0));
                    propertyInfo.setResultType(ResultType.COLLECTION);
                    return;
                }
                if (Set.class.isAssignableFrom((Class<?>) rawType)) {
                    propertyInfo.setJavaType(Set.class);
                    propertyInfo.setOfType(TypeArgumentResolver.resolveTypeArgument(propertyType, Set.class, 0));
                    propertyInfo.setResultType(ResultType.COLLECTION);
                    return;
                }
                if (rawType == Optional.class) {
                    propertyInfo.setJavaType(TypeArgumentResolver.resolveTypeArgument(propertyType, Optional.class, 0));
                    propertyInfo.setResultType(ResultType.ASSOCIATION);
                    return;
                }
            }
        } else if (propertyType instanceof Class) {
            propertyInfo.setJavaType((Class<?>) propertyType);
            propertyInfo.setResultType(ResultType.ASSOCIATION);
            return;
        }
        throw new MybatisExtException("Unsupported property type: " + propertyType);
    }

    private static JoinColumnFeature buildJoinColumnFeature(JoinColumnInfo joinColumnInfo, JoinTableInfo leftJoinTableInfo, JoinTableInfo righJoinTableInfo) {
        JoinColumnFeature joinColumnFeature = new JoinColumnFeature();
        joinColumnFeature.setLeftColumn(joinColumnInfo.getLeftColumn());
        joinColumnFeature.setRightColumn(joinColumnInfo.getRightColumn());
        joinColumnFeature.setLeftTable(leftJoinTableInfo);
        joinColumnFeature.setRightTable(righJoinTableInfo.getTableInfo());
        return joinColumnFeature;
    }
}
