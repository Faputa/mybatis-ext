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

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.EmbedParent;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinParent;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.idgenerator.IdGenerator;
import io.github.mybatisext.resultmap.ResultType;
import io.github.mybatisext.util.StringUtils;
import io.github.mybatisext.util.TypeArgumentResolver;

public class TableInfoFactory {

    private static final Map<Class<?>, TableInfo> tableInfoCache = new ConcurrentHashMap<>();

    // TODO 考虑改为静态方法调用对象方法，缩短参数长度
    public static TableInfo getTableInfo(Configuration configuration, Class<?> tableClass) {
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
        processProperty(configuration, tableClass, tableInfo);
        return tableInfo;
    }

    private static void processProperty(Configuration configuration, Class<?> tableClass, TableInfo tableInfo) {
        AtomicInteger aliasCount = new AtomicInteger(1);
        Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo = new HashMap<>();

        boolean inJoinParent = false;
        for (Class<?> c = tableClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (tableInfo.getNameToPropertyInfo().containsKey(field.getName())) {
                    continue;
                }
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    if (inJoinParent) {
                        processJoinProperty(configuration, tableInfo, c, new JoinRelation[0], column, field.getName(), field.getGenericType(), featureToJoinTableInfo, aliasCount);
                    } else {
                        processColumn(configuration, tableInfo, column, field.getAnnotation(Id.class), field.getName(), field.getGenericType());
                    }
                } else {
                    JoinRelation[] joinRelations = field.getAnnotationsByType(JoinRelation.class);
                    if (joinRelations.length > 0) {
                        processJoinProperty(configuration, tableInfo, c, joinRelations, null, field.getName(), field.getGenericType(), featureToJoinTableInfo, aliasCount);
                    }
                }
            }

            BeanInfo beanInfo;
            try {
                beanInfo = Introspector.getBeanInfo(c, Introspector.IGNORE_ALL_BEANINFO);
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
                if (readMethod.getDeclaringClass() != c) {
                    continue;
                }
                Column column = readMethod.getAnnotation(Column.class);
                if (column != null) {
                    if (inJoinParent) {
                        processJoinProperty(configuration, tableInfo, c, new JoinRelation[0], column, propertyDescriptor.getName(), readMethod.getGenericReturnType(), featureToJoinTableInfo, aliasCount);
                    } else {
                        processColumn(configuration, tableInfo, column, readMethod.getAnnotation(Id.class), propertyDescriptor.getName(), readMethod.getGenericReturnType());
                    }
                } else {
                    JoinRelation[] joinRelations = readMethod.getAnnotationsByType(JoinRelation.class);
                    if (joinRelations.length > 0) {
                        processJoinProperty(configuration, tableInfo, c, joinRelations, null, readMethod.getName(), readMethod.getGenericReturnType(), featureToJoinTableInfo, aliasCount);
                    }
                }
            }

            if (c.isAnnotationPresent(EmbedParent.class)) {
                continue;
            }
            if (!c.isAnnotationPresent(JoinParent.class)) {
                break;
            }
            inJoinParent = true;
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

    private static void processColumn(Configuration configuration, TableInfo tableInfo, Column column, @Nullable Id id, String propertyName, Type propertyType) {
        PropertyInfo propertyInfo = buildColumnPropertyInfo(configuration, tableInfo, column, id, propertyName, propertyType);
        tableInfo.getNameToPropertyInfo().put(propertyName, propertyInfo);
    }

    private static PropertyInfo buildColumnPropertyInfo(Configuration configuration, TableInfo tableInfo, Column column, @Nullable Id id, String propertyName, Type propertyType) {
        String columnName = StringUtils.isNotBlank(column.name()) ? column.name() : StringUtils.camelToSnake(propertyName);
        ColumnInfo columnInfo = new ColumnInfo();
        columnInfo.setName(columnName);
        columnInfo.setComment(column.comment());
        columnInfo.setNullable(column.nullable());
        columnInfo.setColumnDefinition(column.columnDefinition());
        columnInfo.setLength(column.length());
        columnInfo.setPrecision(column.precision());
        columnInfo.setScale(column.scale());
        ColumnInfo existedColumnInfo = tableInfo.getNameToColumnInfo().get(columnName);
        if (existedColumnInfo == null) {
            tableInfo.getNameToColumnInfo().put(columnName, columnInfo);
        } else if (!existedColumnInfo.equals(columnInfo)) {
            throw new MybatisExtException("Inconsistent column definitions: " + columnName);
        }

        PropertyInfo propertyInfo = buildPropertyInfo(configuration, propertyType);
        propertyInfo.setName(propertyName);
        propertyInfo.setColumnName(columnName);
        propertyInfo.setTableInfo(tableInfo);
        propertyInfo.setJdbcType(column.jdbcType());
        propertyInfo.setJoinTableInfo(tableInfo.getJoinTableInfo());
        if (id != null) {
            processIdType(propertyInfo, id);
        }
        if (propertyInfo.getResultType() == ResultType.ASSOCIATION) {
            propertyInfo.getSubPropertyInfos().addAll(collectColumnPropertyInfos(configuration, tableInfo, propertyInfo.getJavaType()));
        } else if (propertyInfo.getResultType() == ResultType.COLLECTION) {
            propertyInfo.getSubPropertyInfos().addAll(collectColumnPropertyInfos(configuration, tableInfo, propertyInfo.getOfType()));
        }
        return propertyInfo;
    }

    private static List<PropertyInfo> collectColumnPropertyInfos(Configuration configuration, TableInfo tableInfo, Class<?> javaType) {
        Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();
        for (Class<?> c = javaType; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (nameToPropertyInfo.containsKey(field.getName())) {
                    continue;
                }
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    PropertyInfo propertyInfo = buildColumnPropertyInfo(configuration, tableInfo, column, field.getAnnotation(Id.class), field.getName(), field.getGenericType());
                    nameToPropertyInfo.put(field.getName(), propertyInfo);
                }
            }
        }

        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(javaType, Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException e) {
            throw new MybatisExtException(e);
        }

        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            if (nameToPropertyInfo.containsKey(propertyDescriptor.getName())) {
                continue;
            }
            Method readMethod = propertyDescriptor.getReadMethod();
            if (readMethod == null) {
                continue;
            }
            Column column = readMethod.getAnnotation(Column.class);
            if (column != null) {
                PropertyInfo propertyInfo = buildColumnPropertyInfo(configuration, tableInfo, column, readMethod.getAnnotation(Id.class), propertyDescriptor.getName(), readMethod.getGenericReturnType());
                nameToPropertyInfo.put(propertyDescriptor.getName(), propertyInfo);
            }
        }
        return new ArrayList<>(nameToPropertyInfo.values());
    }

    private static void processIdType(PropertyInfo propertyInfo, Id id) {
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

    private static void processJoinProperty(Configuration configuration, TableInfo tableInfo, Class<?> currentClass, JoinRelation[] joinRelations, @Nullable Column column, String propertyName, Type propertyType, Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo, AtomicInteger aliasCount) {
        PropertyInfo propertyInfo = buildPropertyInfo(configuration, propertyType);
        propertyInfo.setName(propertyName);
        propertyInfo.setTableInfo(tableInfo);
        tableInfo.getNameToPropertyInfo().put(propertyName, propertyInfo);

        if (column != null) {
            propertyInfo.setColumnName(StringUtils.isNotBlank(column.name()) ? column.name() : StringUtils.camelToSnake(propertyName));
        } else {
            JoinRelation joinRelation = joinRelations[joinRelations.length - 1];
            if (joinRelation.table() != void.class) {
                propertyInfo.setColumnName(StringUtils.isNotBlank(joinRelation.column()) ? joinRelation.column() : StringUtils.camelToSnake(propertyName));
            }
        }

        Map<String, JoinTableInfo> aliasToJoinTableInfo = new HashMap<>();
        buildJoinTableInfos(configuration, tableInfo, currentClass, propertyInfo, joinRelations, aliasToJoinTableInfo);
        checkJoinTableInfos(aliasToJoinTableInfo);
        mergeJoinTableInfos(tableInfo, propertyInfo, featureToJoinTableInfo, aliasCount);
    }

    private static void buildJoinTableInfos(Configuration configuration, TableInfo rootTableInfo, Class<?> currentClass, PropertyInfo propertyInfo, JoinRelation[] joinRelations, Map<String, JoinTableInfo> aliasToJoinTableInfo) {
        JoinTableInfo lastJoinTableInfo = rootTableInfo.getJoinTableInfo();
        JoinTableInfo parentJoinTableInfo = lastJoinTableInfo;
        Class<?> tableClass = rootTableInfo.getTableClass();

        for (Class<?> c = tableClass; currentClass.isAssignableFrom(c) && c != Object.class; c = c.getSuperclass()) {
            if (c.isAnnotationPresent(EmbedParent.class)) {
                continue;
            }
            if (!c.isAnnotationPresent(JoinParent.class)) {
                break;
            }
            JoinParent joinParent = c.getAnnotation(JoinParent.class);
            TableInfo tableInfo = getTableInfo(configuration, c.getSuperclass());
            lastJoinTableInfo = parentJoinTableInfo;
            parentJoinTableInfo = new JoinTableInfo();
            parentJoinTableInfo.setTableInfo(tableInfo);
            parentJoinTableInfo.setAlias(joinParent.alias());

            for (JoinColumn joinColumn : joinParent.joinColumn()) {
                JoinColumnInfo joinColumnInfo = new JoinColumnInfo();
                joinColumnInfo.setLeftColumn(joinColumn.leftColumn());
                joinColumnInfo.setRightColumn(joinColumn.rightColumn());
                lastJoinTableInfo.getRightJoinTableInfos().put(joinColumnInfo, parentJoinTableInfo);
                parentJoinTableInfo.getLeftJoinTableInfos().put(joinColumnInfo, lastJoinTableInfo);
            }
        }

        if (StringUtils.isNotBlank(lastJoinTableInfo.getAlias())) {
            aliasToJoinTableInfo.put(lastJoinTableInfo.getAlias(), lastJoinTableInfo);
        }
        if (parentJoinTableInfo != lastJoinTableInfo && StringUtils.isNotBlank(parentJoinTableInfo.getAlias())) {
            if (aliasToJoinTableInfo.containsKey(parentJoinTableInfo.getAlias())) {
                throw new MybatisExtException("Duplicate table alias: " + parentJoinTableInfo.getAlias());
            }
            aliasToJoinTableInfo.put(parentJoinTableInfo.getAlias(), parentJoinTableInfo);
        }

        if (joinRelations.length > 0) {
            processJoinRelations(configuration, lastJoinTableInfo, propertyInfo, joinRelations, aliasToJoinTableInfo);
        } else {
            propertyInfo.setJoinTableInfo(parentJoinTableInfo);
        }
    }

    private static void processJoinRelations(Configuration configuration, JoinTableInfo lastJoinTableInfo, PropertyInfo propertyInfo, JoinRelation[] joinRelations, Map<String, JoinTableInfo> aliasToJoinTableInfo) {
        for (JoinRelation joinRelation : joinRelations) {
            JoinTableInfo joinTableInfo;
            TableInfo tableInfo = resolveTableInfoFromJoinRelation(configuration, propertyInfo, joinRelation);
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
                    lastJoinTableInfo.getRightJoinTableInfos().put(joinColumnInfo, joinTableInfo);
                    joinTableInfo.getLeftJoinTableInfos().put(joinColumnInfo, lastJoinTableInfo);
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

                Set<JoinColumnFeature> joinColumnFeatures = joinTableInfo.getLeftJoinTableInfos().entrySet().stream().map(v -> buildJoinColumnFeature(v.getKey(), v.getValue(), joinTableInfo)).collect(Collectors.toSet());
                JoinTableInfo existedJoinTableInfo = featureToJoinTableInfo.get(joinColumnFeatures);

                if (existedJoinTableInfo != null) {
                    if (existedJoinTableInfo == joinTableInfo) {
                        continue;
                    }
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

    private static TableInfo resolveTableInfoFromJoinRelation(Configuration configuration, PropertyInfo propertyInfo, JoinRelation joinRelation) {
        if (joinRelation.table() != void.class) {
            return getTableInfo(configuration, joinRelation.table());
        }
        if (propertyInfo.getOfType() != null) {
            return getTableInfo(configuration, propertyInfo.getOfType());
        }
        return getTableInfo(configuration, propertyInfo.getJavaType());
    }

    private static PropertyInfo buildPropertyInfo(Configuration configuration, Type propertyType) {
        PropertyInfo propertyInfo = new PropertyInfo();
        if (propertyType instanceof ParameterizedType && ((ParameterizedType) propertyType).getRawType() instanceof Class) {
            Class<?> typeClass = (Class<?>) ((ParameterizedType) propertyType).getRawType();
            if (List.class.isAssignableFrom(typeClass)) {
                propertyInfo.setJavaType(typeClass);
                propertyInfo.setOfType(TypeArgumentResolver.resolveTypeArgument(propertyType, List.class, 0));
                propertyInfo.setResultType(ResultType.COLLECTION);
            } else if (Set.class.isAssignableFrom(typeClass)) {
                propertyInfo.setJavaType(typeClass);
                propertyInfo.setOfType(TypeArgumentResolver.resolveTypeArgument(propertyType, Set.class, 0));
                propertyInfo.setResultType(ResultType.COLLECTION);
            } else if (typeClass == Optional.class) {
                propertyInfo.setJavaType(TypeArgumentResolver.resolveTypeArgument(propertyType, Optional.class, 0));
            }
        } else if (propertyType instanceof Class) {
            propertyInfo.setJavaType((Class<?>) propertyType);
        }
        if (propertyInfo.getJavaType() == null) {
            throw new MybatisExtException("Unsupported property type: " + propertyType);
        }
        if (propertyInfo.getResultType() == null) {
            if (configuration.getTypeHandlerRegistry().hasTypeHandler(propertyInfo.getJavaType())) {
                propertyInfo.setResultType(ResultType.RESULT);
            } else {
                propertyInfo.setResultType(ResultType.ASSOCIATION);
            }
        }
        return propertyInfo;
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
