package io.github.mybatisext.metadata;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import io.github.mybatisext.annotation.LoadStrategy;
import io.github.mybatisext.annotation.LoadType;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.idgenerator.IdGenerator;
import io.github.mybatisext.reflect.GenericField;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.util.StringUtils;
import io.github.mybatisext.util.TypeArgumentResolver;

public class TableInfoFactory {

    private static final Map<GenericType, TableInfo> tableInfoCache = new ConcurrentHashMap<>();

    public static TableInfo getTableInfo(Configuration configuration, Class<?> tableClass) {
        return getTableInfo(configuration, GenericTypeFactory.build(tableClass));
    }

    public static TableInfo getTableInfo(Configuration configuration, GenericType tableClass) {
        for (GenericType c = tableClass; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            if (c.isAnnotationPresent(Table.class)) {
                return processTable(configuration, c, c.getAnnotation(Table.class));
            }
        }
        throw new MybatisExtException("Class [" + tableClass.getTypeName() + "] lacks @" + Table.class.getSimpleName()/* + " or @" + TableRef.class.getSimpleName() */ + " annotation.");
    }

    private static TableInfo processTable(Configuration configuration, GenericType tableClass, Table table) {
        if (tableInfoCache.containsKey(tableClass)) {
            return tableInfoCache.get(tableClass);
        }
        TableInfo tableInfo = new TableInfo();
        tableInfoCache.put(tableClass, tableInfo);
        JoinTableInfo joinTableInfo = new JoinTableInfo();
        joinTableInfo.setTableInfo(tableInfo);
        joinTableInfo.setAlias(table.alias());
        tableInfo.setJoinTableInfo(joinTableInfo);
        tableInfo.setTableClass(tableClass);
        tableInfo.setName(table.name());
        tableInfo.setComment(table.comment());
        tableInfo.setSchema(table.schema());
        if (StringUtils.isBlank(tableInfo.getName())) {
            tableInfo.setName(StringUtils.camelToSnake(tableClass.getSimpleName()));
        }
        if (StringUtils.isNotBlank(joinTableInfo.getAlias())) {
            tableInfo.getAliasToJoinTableInfo().put(joinTableInfo.getAlias(), joinTableInfo);
        }
        processProperty(configuration, tableClass, tableInfo);
        return tableInfo;
    }

    private static void processProperty(Configuration configuration, GenericType tableClass, TableInfo tableInfo) {
        AtomicInteger aliasCount = new AtomicInteger(1);
        Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo = new HashMap<>();

        boolean inJoinParent = false;
        for (GenericType c = tableClass; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            for (GenericField field : c.getDeclaredFields()) {
                if (tableInfo.getNameToPropertyInfo().containsKey(field.getName())) {
                    continue;
                }
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    if (inJoinParent) {
                        processJoinProperty(configuration, tableInfo, c, new JoinRelation[0], column, field.getAnnotation(LoadStrategy.class), field.getName(), field.getGenericType(), false, featureToJoinTableInfo, aliasCount);
                    } else {
                        processColumn(configuration, tableInfo, column, field.getAnnotation(Id.class), field.getName(), field.getGenericType(), false);
                    }
                } else {
                    JoinRelation[] joinRelations = field.getAnnotationsByType(JoinRelation.class);
                    if (joinRelations.length > 0) {
                        processJoinProperty(configuration, tableInfo, c, joinRelations, null, field.getAnnotation(LoadStrategy.class), field.getName(), field.getGenericType(), false, featureToJoinTableInfo, aliasCount);
                    }
                }
            }

            BeanInfo beanInfo;
            try {
                beanInfo = Introspector.getBeanInfo(c.getType(), Introspector.IGNORE_ALL_BEANINFO);
            } catch (IntrospectionException e) {
                throw new MybatisExtException(e);
            }

            Map<Method, GenericMethod> methodMap = Arrays.stream(c.getMethods()).collect(Collectors.toMap(GenericMethod::getMethod, v -> v));
            for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                if (tableInfo.getNameToPropertyInfo().containsKey(propertyDescriptor.getName())) {
                    continue;
                }
                GenericMethod readMethod = methodMap.get(propertyDescriptor.getReadMethod());
                if (readMethod == null || readMethod.getDeclaringClass() != c.getType()) {
                    continue;
                }
                Column column = readMethod.getAnnotation(Column.class);
                if (column != null) {
                    if (inJoinParent) {
                        processJoinProperty(configuration, tableInfo, c, new JoinRelation[0], column, readMethod.getAnnotation(LoadStrategy.class), propertyDescriptor.getName(), readMethod.getGenericReturnType(), propertyDescriptor.getWriteMethod() == null, featureToJoinTableInfo, aliasCount);
                    } else {
                        processColumn(configuration, tableInfo, column, readMethod.getAnnotation(Id.class), propertyDescriptor.getName(), readMethod.getGenericReturnType(), propertyDescriptor.getWriteMethod() == null);
                    }
                } else {
                    JoinRelation[] joinRelations = readMethod.getAnnotationsByType(JoinRelation.class);
                    if (joinRelations.length > 0) {
                        processJoinProperty(configuration, tableInfo, c, joinRelations, null, readMethod.getAnnotation(LoadStrategy.class), propertyDescriptor.getName(), readMethod.getGenericReturnType(), propertyDescriptor.getWriteMethod() == null, featureToJoinTableInfo, aliasCount);
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

    private static void processColumn(Configuration configuration, TableInfo tableInfo, Column column, @Nullable Id id, String propertyName, GenericType propertyType, boolean readonly) {
        PropertyInfo propertyInfo = buildColumnPropertyInfo(configuration, tableInfo, column, id, propertyName, propertyType, readonly);
        tableInfo.getNameToPropertyInfo().put(propertyName, propertyInfo);
    }

    private static PropertyInfo buildColumnPropertyInfo(Configuration configuration, TableInfo tableInfo, Column column, @Nullable Id id, String propertyName, GenericType propertyType, boolean readonly) {
        PropertyInfo propertyInfo = buildPropertyInfo(configuration, propertyType);
        propertyInfo.setName(propertyName);
        propertyInfo.setOwnColumn(true);
        propertyInfo.setReadonly(readonly);
        propertyInfo.setJdbcType(column.jdbcType());
        propertyInfo.setJoinTableInfo(tableInfo.getJoinTableInfo());
        if (propertyInfo.getResultType() == ResultType.ASSOCIATION) {
            propertyInfo.putAll(collectColumnPropertyInfos(configuration, tableInfo, GenericTypeFactory.build(propertyInfo.getJavaType()), readonly));
        } else if (propertyInfo.getResultType() == ResultType.COLLECTION) {
            propertyInfo.putAll(collectColumnPropertyInfos(configuration, tableInfo, GenericTypeFactory.build(propertyInfo.getOfType()), readonly));
        } else {
            if (id != null) {
                processIdType(propertyInfo, id);
            }
            String columnName = StringUtils.isNotBlank(column.name()) ? column.name() : StringUtils.camelToSnake(propertyName);
            propertyInfo.setColumnName(columnName);
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
        }
        return propertyInfo;
    }

    private static Map<String, PropertyInfo> collectColumnPropertyInfos(Configuration configuration, TableInfo tableInfo, GenericType javaType, boolean readonly) {
        Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();
        for (GenericType c = javaType; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            for (GenericField field : c.getDeclaredFields()) {
                if (nameToPropertyInfo.containsKey(field.getName())) {
                    continue;
                }
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    PropertyInfo propertyInfo = buildColumnPropertyInfo(configuration, tableInfo, column, field.getAnnotation(Id.class), field.getName(), field.getGenericType(), readonly);
                    nameToPropertyInfo.put(field.getName(), propertyInfo);
                }
            }
        }

        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(javaType.getType(), Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException e) {
            throw new MybatisExtException(e);
        }

        Map<Method, GenericMethod> methodMap = Arrays.stream(javaType.getMethods()).collect(Collectors.toMap(GenericMethod::getMethod, v -> v));
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            if (nameToPropertyInfo.containsKey(propertyDescriptor.getName())) {
                continue;
            }
            GenericMethod readMethod = methodMap.get(propertyDescriptor.getReadMethod());
            if (readMethod == null) {
                continue;
            }
            Column column = readMethod.getAnnotation(Column.class);
            if (column != null) {
                PropertyInfo propertyInfo = buildColumnPropertyInfo(configuration, tableInfo, column, readMethod.getAnnotation(Id.class), propertyDescriptor.getName(), readMethod.getGenericReturnType(), readonly || propertyDescriptor.getWriteMethod() == null);
                nameToPropertyInfo.put(propertyDescriptor.getName(), propertyInfo);
            }
        }
        return nameToPropertyInfo;
    }

    private static void processIdType(PropertyInfo propertyInfo, Id id) {
        if (id.idType() == IdType.CUSTOM) {
            if (id.customIdGenerator() == void.class) {
                throw new MybatisExtException("customIdGenerator cannot be null");
            }
            if (!IdGenerator.class.isAssignableFrom(id.customIdGenerator())) {
                throw new MybatisExtException("customIdGenerator must implement the IdGenerator");
            }
            propertyInfo.setCustomIdGenerator(id.customIdGenerator());
        }
        propertyInfo.setIdType(id.idType());
        propertyInfo.setResultType(ResultType.ID);
    }

    private static void processJoinProperty(Configuration configuration, TableInfo tableInfo, GenericType currentClass, JoinRelation[] joinRelations, @Nullable Column column, @Nullable LoadStrategy loadStrategy, String propertyName, GenericType propertyType, boolean readonly, Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo, AtomicInteger aliasCount) {
        PropertyInfo propertyInfo = buildPropertyInfo(configuration, propertyType);
        propertyInfo.setName(propertyName);
        propertyInfo.setOwnColumn(false);
        propertyInfo.setReadonly(readonly);
        tableInfo.getNameToPropertyInfo().put(propertyName, propertyInfo);

        if (column != null) {
            propertyInfo.setColumnName(StringUtils.isNotBlank(column.name()) ? column.name() : StringUtils.camelToSnake(propertyName));
        } else {
            JoinRelation joinRelation = joinRelations[joinRelations.length - 1];
            if (joinRelation.table() != void.class) {
                propertyInfo.setColumnName(StringUtils.isNotBlank(joinRelation.column()) ? joinRelation.column() : StringUtils.camelToSnake(propertyName));
            }
        }
        if (loadStrategy != null) {
            propertyInfo.setLoadType(loadStrategy.value());
            if (propertyInfo.getResultType() == ResultType.RESULT && propertyInfo.getLoadType() != LoadType.JOIN) {
                propertyInfo.setResultType(ResultType.ASSOCIATION);
            }
        }

        buildJoinTableInfos(configuration, tableInfo, currentClass, propertyInfo, joinRelations);
        mergeJoinTableInfos(tableInfo, propertyInfo, featureToJoinTableInfo, aliasCount);

        if (propertyInfo.getColumnName() == null &&
                (propertyInfo.getResultType() == ResultType.ASSOCIATION || propertyInfo.getResultType() == ResultType.COLLECTION) &&
                (propertyInfo.getLoadType() == null || propertyInfo.getLoadType() == LoadType.JOIN)) {
            propertyInfo.putAll(collectJoinTablePropertyInfos(propertyInfo.getJoinTableInfo()));
        }
    }

    private static void buildJoinTableInfos(Configuration configuration, TableInfo rootTableInfo, GenericType currentClass, PropertyInfo propertyInfo, JoinRelation[] joinRelations) {
        JoinTableInfo lastJoinTableInfo = rootTableInfo.getJoinTableInfo();
        JoinTableInfo parentJoinTableInfo = rootTableInfo.getJoinTableInfo();
        GenericType tableClass = rootTableInfo.getTableClass();

        for (GenericType c = tableClass; currentClass.isAssignableFrom(c) && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            lastJoinTableInfo = parentJoinTableInfo;
            if (c.isAnnotationPresent(EmbedParent.class)) {
                continue;
            }
            if (!c.isAnnotationPresent(JoinParent.class) || !c.isAnnotationPresent(Table.class)) {
                break;
            }
            JoinParent joinParent = c.getAnnotation(JoinParent.class);
            TableInfo tableInfo = getTableInfo(configuration, c.getGenericSuperclass());
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

        Map<String, JoinTableInfo> aliasToJoinTableInfo = new HashMap<>();
        if (StringUtils.isNotBlank(lastJoinTableInfo.getAlias())) {
            aliasToJoinTableInfo.put(lastJoinTableInfo.getAlias(), lastJoinTableInfo);
        }
        if (parentJoinTableInfo != lastJoinTableInfo && !parentJoinTableInfo.getAlias().isEmpty()) {
            if (parentJoinTableInfo.getAlias().equals(lastJoinTableInfo.getAlias())) {
                throw new MybatisExtException("Duplicate table alias: " + parentJoinTableInfo.getAlias());
            }
            aliasToJoinTableInfo.put(parentJoinTableInfo.getAlias(), parentJoinTableInfo);
        }

        processJoinRelations(configuration, lastJoinTableInfo, propertyInfo, joinRelations, aliasToJoinTableInfo);
        checkJoinTableInfos(aliasToJoinTableInfo);
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
                } else {

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

    private static PropertyInfo buildPropertyInfo(Configuration configuration, GenericType propertyType) {
        PropertyInfo propertyInfo = new PropertyInfo();
        if (Collection.class.isAssignableFrom(propertyType.getType())) {
            propertyInfo.setJavaType(propertyType);
            propertyInfo.setOfType(TypeArgumentResolver.resolveGenericTypeArgument(propertyType, Collection.class, 0));
            propertyInfo.setResultType(ResultType.COLLECTION);
        } else if (propertyType.getType() == Optional.class) {
            propertyInfo.setJavaType(TypeArgumentResolver.resolveGenericTypeArgument(propertyType, Collection.class, 0));
        } else if (propertyType.getTypeParameters().length == 0) {
            propertyInfo.setJavaType(propertyType);
        }
        if (propertyInfo.getJavaType() == null) {
            throw new MybatisExtException("Unsupported property type: " + propertyType);
        }
        if (propertyInfo.getResultType() == null) {
            if (configuration.getTypeHandlerRegistry().hasTypeHandler(propertyInfo.getJavaType().getType())) {
                propertyInfo.setResultType(ResultType.RESULT);
            } else {
                propertyInfo.setResultType(ResultType.ASSOCIATION);
            }
        }
        return propertyInfo;
    }

    private static Map<String, PropertyInfo> collectJoinTablePropertyInfos(JoinTableInfo joinTableInfo) {
        Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();
        for (PropertyInfo propertyInfo : joinTableInfo.getTableInfo().getNameToPropertyInfo().values()) {
            if (!propertyInfo.isOwnColumn()) {
                continue;
            }
            PropertyInfo newPropertyInfo = copyJoinTablePropertyInfo(propertyInfo);
            newPropertyInfo.setJoinTableInfo(joinTableInfo);
            newPropertyInfo.setOwnColumn(false);
            newPropertyInfo.putAll(collectJoinTablePropertyInfos(joinTableInfo, propertyInfo));
            nameToPropertyInfo.put(propertyInfo.getName(), newPropertyInfo);
        }
        return nameToPropertyInfo;
    }

    private static Map<String, PropertyInfo> collectJoinTablePropertyInfos(JoinTableInfo joinTableInfo, PropertyInfo propertyInfo) {
        Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();
        for (PropertyInfo value : propertyInfo.values()) {
            if (!value.isOwnColumn()) {
                continue;
            }
            PropertyInfo newPropertyInfo = copyJoinTablePropertyInfo(propertyInfo);
            newPropertyInfo.setJoinTableInfo(joinTableInfo);
            newPropertyInfo.setOwnColumn(false);
            newPropertyInfo.putAll(collectJoinTablePropertyInfos(joinTableInfo, propertyInfo));
            nameToPropertyInfo.put(propertyInfo.getName(), newPropertyInfo);
        }
        return nameToPropertyInfo;
    }

    private static PropertyInfo copyJoinTablePropertyInfo(PropertyInfo propertyInfo) {
        PropertyInfo newPropertyInfo = new PropertyInfo();
        newPropertyInfo.setName(propertyInfo.getName());
        newPropertyInfo.setColumnName(propertyInfo.getColumnName());
        newPropertyInfo.setJavaType(propertyInfo.getJavaType());
        newPropertyInfo.setJdbcType(propertyInfo.getJdbcType());
        newPropertyInfo.setReadonly(propertyInfo.isReadonly());
        newPropertyInfo.setResultType(propertyInfo.getResultType());
        newPropertyInfo.setIdType(propertyInfo.getIdType());
        newPropertyInfo.setCustomIdGenerator(propertyInfo.getCustomIdGenerator());
        return newPropertyInfo;
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
