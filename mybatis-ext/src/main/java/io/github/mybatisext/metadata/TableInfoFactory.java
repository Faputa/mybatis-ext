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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.ColumnRef;
import io.github.mybatisext.annotation.EmbedParent;
import io.github.mybatisext.annotation.Filterable;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinParent;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.LoadStrategy;
import io.github.mybatisext.annotation.LoadType;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.idgenerator.IdGenerator;
import io.github.mybatisext.jpa.CompareOperator;
import io.github.mybatisext.jpa.LogicalOperator;
import io.github.mybatisext.reflect.GenericField;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.util.CommonUtils;
import io.github.mybatisext.util.StringUtils;
import io.github.mybatisext.util.TypeArgumentResolver;

public class TableInfoFactory {

    private static final Map<GenericType, TableInfo> tableInfoCache = new ConcurrentHashMap<>();
    private final Configuration configuration;
    private final ExtContext extContext;

    public TableInfoFactory(Configuration configuration, ExtContext extContext) {
        this.configuration = configuration;
        this.extContext = extContext;
    }

    public static PropertyInfo deepGet(TableInfo tableInfo, String fullName) {
        Map<String, PropertyInfo> map = tableInfo.getNameToPropertyInfo();
        PropertyInfo propertyInfo = null;
        for (String key : fullName.split("\\.")) {
            if ((propertyInfo = map.get(key)) == null) {
                return null;
            }
            map = propertyInfo;
        }
        return propertyInfo;
    }

    public static boolean isAssignableEitherWithTable(GenericType left, GenericType right) {
        return isAssignableFromWithTable(left, right) || isAssignableFromWithTable(right, left);
    }

    public static boolean isAssignableFromWithTable(GenericType left, GenericType right) {
        if (left.isAssignableFrom(right)) {
            return true;
        }
        if ((left = getTableAnnotationClass(left)) == null) {
            return false;
        }
        if ((right = getTableAnnotationClass(right)) == null) {
            return false;
        }
        return left.isAssignableFrom(right);
    }

    public static GenericType getTableAnnotationClass(GenericType genericType) {
        for (GenericType c = genericType; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            if (c.isAnnotationPresent(Table.class)) {
                return c;
            }
            if (c.isAnnotationPresent(TableRef.class)) {
                return GenericTypeFactory.build(c.getAnnotation(TableRef.class).value());
            }
        }
        return null;
    }

    public TableInfo getTableInfo(Class<?> tableClass) {
        return getTableInfo(GenericTypeFactory.build(tableClass));
    }

    public TableInfo getTableInfo(@Nonnull GenericType tableClass) {
        for (GenericType c = tableClass; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            if (c.isAnnotationPresent(Table.class)) {
                return processTable(c, c.getAnnotation(Table.class));
            }
            if (c.isAnnotationPresent(TableRef.class)) {
                return processTableRef(c, c.getAnnotation(TableRef.class));
            }
        }
        throw new MybatisExtException("Class [" + tableClass.getName() + "] lacks @" + Table.class.getSimpleName() + " or @" + TableRef.class.getSimpleName() + " annotation.");
    }

    private TableInfo processTableRef(GenericType tableClass, TableRef tableRef) {
        if (tableInfoCache.containsKey(tableClass)) {
            return tableInfoCache.get(tableClass);
        }
        TableInfo refTableInfo = getTableInfo(tableRef.value());
        TableInfo tableInfo = new TableInfo();
        tableInfoCache.put(tableClass, tableInfo);
        tableInfo.setTableClass(tableClass);
        tableInfo.setJoinTableInfo(refTableInfo.getJoinTableInfo());
        tableInfo.setName(refTableInfo.getName());
        tableInfo.setComment(refTableInfo.getComment());
        tableInfo.setSchema(refTableInfo.getSchema());
        tableInfo.getAliasToJoinTableInfo().putAll(refTableInfo.getAliasToJoinTableInfo());
        tableInfo.getNameToColumnInfo().putAll(refTableInfo.getNameToColumnInfo());
        processPropertyRef(tableClass, tableInfo, refTableInfo);
        return tableInfo;
    }

    private void processPropertyRef(GenericType genericType, TableInfo tableInfo, TableInfo refTableInfo) {
        for (GenericType c = genericType; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            for (GenericField field : c.getDeclaredFields()) {
                if (tableInfo.getNameToPropertyInfo().containsKey(field.getName())) {
                    continue;
                }
                ColumnRef columnRef = field.getAnnotation(ColumnRef.class);
                if (columnRef != null) {
                    processRefPropertyInfo(field.getName(), tableInfo, StringUtils.isNotBlank(columnRef.value()) ? columnRef.value() : field.getName(), refTableInfo, field.getGenericType(), field.getAnnotation(Filterable.class), false);
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
                ColumnRef columnRef = readMethod.getAnnotation(ColumnRef.class);
                if (columnRef != null) {
                    processRefPropertyInfo(propertyDescriptor.getName(), tableInfo, StringUtils.isNotBlank(columnRef.value()) ? columnRef.value() : propertyDescriptor.getName(), refTableInfo, readMethod.getGenericReturnType(), readMethod.getAnnotation(Filterable.class), propertyDescriptor.getWriteMethod() == null);
                }
            }
        }
    }

    private void processRefPropertyInfo(String name, TableInfo tableInfo, String refName, TableInfo refTableInfo, GenericType propertyType, Filterable filterable, boolean readonly) {
        PropertyInfo refPropertyInfo = refTableInfo.getNameToPropertyInfo().get(refName);
        if (refPropertyInfo == null) {
            throw new MybatisExtException("Missing property [" + refName + "] in ref table.");
        }
        if (!isAssignableEitherWithTable(CommonUtils.unwrapType(propertyType), CommonUtils.unwrapType(refPropertyInfo.getJavaType()))) {
            throw new MybatisExtException("Type mismatch for property [" + name + "]: expected [" + CommonUtils.unwrapType(propertyType).getTypeName() + "], found [" + CommonUtils.unwrapType(refPropertyInfo.getJavaType()).getTypeName() + "].");
        }
        PropertyInfo propertyInfo = new PropertyInfo(name);
        tableInfo.getNameToPropertyInfo().put(name, propertyInfo);
        propertyInfo.setColumnName(refPropertyInfo.getColumnName());
        propertyInfo.setJoinTableInfo(refPropertyInfo.getJoinTableInfo());
        propertyInfo.setJdbcType(refPropertyInfo.getJdbcType());
        propertyInfo.setOwnColumn(refPropertyInfo.isOwnColumn());
        propertyInfo.setReadonly(readonly);
        propertyInfo.setIdType(refPropertyInfo.getIdType());
        propertyInfo.setCustomIdGenerator(refPropertyInfo.getCustomIdGenerator());
        propertyInfo.setLoadType(refPropertyInfo.getLoadType());

        GenericType targetType;
        if (Collection.class.isAssignableFrom(propertyType.getType())) {
            propertyInfo.setResultType(ResultType.COLLECTION);
            propertyInfo.setJavaType(propertyType);
            propertyInfo.setOfType(targetType = TypeArgumentResolver.resolveGenericType(propertyType, Collection.class, 0));
        } else {
            propertyInfo.setResultType(configuration.getTypeHandlerRegistry().hasTypeHandler(propertyType.getType()) ? ResultType.RESULT : ResultType.ASSOCIATION);
            propertyInfo.setJavaType(targetType = propertyType);
        }
        propertyInfo.setFilterableInfo(buildFilterableInfo(filterable));

        if (propertyInfo.isOwnColumn()) {
            propertyInfo.putAll(refPropertyInfo);
        } else if (getTableAnnotationClass(targetType) != null) {
            propertyInfo.putAll(collectJoinTablePropertyInfos(refPropertyInfo.getJoinTableInfo(), getTableInfo(targetType).getNameToPropertyInfo().values(), propertyInfo.getFullName()));
        }
    }

    private TableInfo processTable(GenericType tableClass, Table table) {
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
        tableInfo.setName(StringUtils.isNotBlank(table.name()) ? table.name() : StringUtils.camelToSnake(tableClass.getSimpleName()));
        tableInfo.setComment(table.comment());
        tableInfo.setSchema(table.schema());

        AtomicInteger aliasCount = new AtomicInteger(1);
        Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo = new HashMap<>();
        processParentJoinRelations(tableClass, tableInfo, featureToJoinTableInfo);
        processProperty(tableClass, tableInfo, featureToJoinTableInfo, aliasCount);
        replaceJoinColumnPlaceholder(tableInfo);

        aliasCount.set(0);
        String alias = joinTableInfo.getAlias();
        while (StringUtils.isBlank(alias) || (!tableClass.isAnnotationPresent(EmbedParent.class) && tableInfo.getAliasToJoinTableInfo().containsKey(alias))) {
            alias = "t" + aliasCount.getAndIncrement();
        }
        joinTableInfo.setAlias(alias);
        tableInfo.getAliasToJoinTableInfo().put(alias, joinTableInfo);
        return tableInfo;
    }

    private static void replaceJoinColumnPlaceholder(TableInfo tableInfo) {
        for (JoinTableInfo joinTableInfo : tableInfo.getAliasToJoinTableInfo().values()) {
            for (Map.Entry<JoinColumnInfo, JoinTableInfo> entry : joinTableInfo.getLeftJoinTableInfos().entrySet()) {
                {
                    PropertyInfo propertyInfo = deepGet(entry.getValue().getTableInfo(), entry.getKey().getLeftColumn().getFullName());
                    if (propertyInfo == null || !propertyInfo.isOwnColumn() || StringUtils.isBlank(propertyInfo.getColumnName())) {
                        throw new MybatisExtException("Column property '" + entry.getKey().getLeftColumn().getFullName() + "': not found in '" + entry.getValue().getTableInfo().getTableClass() + "'.");
                    }
                    PropertyInfo leftColumn = new PropertyInfo(propertyInfo.getPrefix(), propertyInfo.getName());
                    copyPropertyInfoProperties(leftColumn, propertyInfo);
                    leftColumn.setJoinTableInfo(entry.getValue());
                    leftColumn.setOwnColumn(entry.getValue().getTableInfo() == tableInfo);
                    entry.getKey().setLeftColumn(leftColumn);
                }
                {
                    PropertyInfo propertyInfo = deepGet(joinTableInfo.getTableInfo(), entry.getKey().getRightColumn().getFullName());
                    if (propertyInfo == null || !propertyInfo.isOwnColumn() || StringUtils.isBlank(propertyInfo.getColumnName())) {
                        throw new MybatisExtException("Column property '" + entry.getKey().getRightColumn().getFullName() + "': not found in '" + joinTableInfo.getTableInfo().getTableClass() + "'.");
                    }
                    PropertyInfo rightColumn = new PropertyInfo(propertyInfo.getPrefix(), propertyInfo.getName());
                    copyPropertyInfoProperties(rightColumn, propertyInfo);
                    rightColumn.setJoinTableInfo(entry.getValue());
                    rightColumn.setOwnColumn(entry.getValue().getTableInfo() == tableInfo);
                    entry.getKey().setRightColumn(rightColumn);
                }
            }
        }
    }

    private void processParentJoinRelations(@Nonnull GenericType tableClass, TableInfo tableInfo, Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo) {
        if (tableClass.isAnnotationPresent(JoinParent.class)) {
            if (tableClass.getGenericSuperclass() == null || tableClass.getGenericSuperclass().getType() == Object.class) {
                throw new MybatisExtException("@" + JoinParent.class.getSimpleName() + " requires a valid non-Object superclass: " + tableClass.getType().getName());
            }
            TableInfo parentTableInfo = getTableInfo(tableClass.getGenericSuperclass());
            JoinParent joinParent = tableClass.getAnnotation(JoinParent.class);
            for (JoinColumn joinColumn : joinParent.joinColumn()) {
                JoinColumnInfo joinColumnInfo = new JoinColumnInfo();
                joinColumnInfo.setLeftColumn(new PropertyInfo(joinColumn.leftColumn()));
                joinColumnInfo.setRightColumn(new PropertyInfo(joinColumn.rightColumn()));
                tableInfo.getJoinTableInfo().getRightJoinTableInfos().put(joinColumnInfo, parentTableInfo.getJoinTableInfo());
            }
            stripJoinRelations(tableInfo, featureToJoinTableInfo);
        } else {
            for (GenericType c = tableClass; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
                if (!c.isAnnotationPresent(EmbedParent.class)) {
                    break;
                }
                if (c.getGenericSuperclass() == null || tableClass.getGenericSuperclass().getType() == Object.class) {
                    throw new MybatisExtException("@" + EmbedParent.class.getSimpleName() + " requires a valid non-Object superclass: " + c.getType().getName());
                }
                if (c.getGenericSuperclass().isAnnotationPresent(Table.class)) {
                    TableInfo parentTableInfo = getTableInfo(tableClass.getGenericSuperclass());
                    tableInfo.getJoinTableInfo().setAlias(parentTableInfo.getJoinTableInfo().getAlias());
                    tableInfo.getAliasToJoinTableInfo().put(parentTableInfo.getJoinTableInfo().getAlias(), tableInfo.getJoinTableInfo());
                    tableInfo.getJoinTableInfo().getRightJoinTableInfos().putAll(parentTableInfo.getJoinTableInfo().getRightJoinTableInfos());
                    stripJoinRelations(tableInfo, featureToJoinTableInfo);
                    break;
                }
            }
        }
    }

    private static void stripJoinRelations(TableInfo tableInfo, Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo) {
        List<JoinTableInfo> queue = new ArrayList<>();
        queue.add(tableInfo.getJoinTableInfo());
        for (int i = 0; i < queue.size(); i++) {
            JoinTableInfo leftJoinTableInfo = queue.get(i);
            for (JoinTableInfo rightJoinTableInfo : leftJoinTableInfo.getRightJoinTableInfos().values()) {
                if (tableInfo.getAliasToJoinTableInfo().containsKey(rightJoinTableInfo.getAlias())) {
                    continue;
                }
                JoinTableInfo joinTableInfo = new JoinTableInfo();
                joinTableInfo.setAlias(rightJoinTableInfo.getAlias());
                joinTableInfo.setTableInfo(rightJoinTableInfo.getTableInfo());
                tableInfo.getAliasToJoinTableInfo().put(rightJoinTableInfo.getAlias(), joinTableInfo);
                queue.add(joinTableInfo);

                for (JoinColumnInfo joinColumnInfo : leftJoinTableInfo.getRightJoinTableInfos().keySet()) {
                    leftJoinTableInfo.getRightJoinTableInfos().put(joinColumnInfo, joinTableInfo);
                    joinTableInfo.getLeftJoinTableInfos().put(joinColumnInfo, leftJoinTableInfo);
                }
                joinTableInfo.getRightJoinTableInfos().putAll(rightJoinTableInfo.getRightJoinTableInfos());

                Set<JoinColumnFeature> joinColumnFeatures = joinTableInfo.getLeftJoinTableInfos().entrySet().stream().map(v -> buildJoinColumnFeature(v.getKey(), v.getValue(), joinTableInfo)).collect(Collectors.toSet());
                featureToJoinTableInfo.put(joinColumnFeatures, joinTableInfo);
            }
        }
    }

    private void processProperty(GenericType tableClass, TableInfo tableInfo, Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo, AtomicInteger aliasCount) {
        boolean inJoinParent = false;
        for (GenericType c = tableClass; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            for (GenericField field : c.getDeclaredFields()) {
                if (tableInfo.getNameToPropertyInfo().containsKey(field.getName())) {
                    continue;
                }
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    if (inJoinParent) {
                        processJoinProperty(tableInfo, c, new JoinRelation[0], column, field.getAnnotation(LoadStrategy.class), field.getAnnotation(Filterable.class), field.getName(), field.getGenericType(), false, featureToJoinTableInfo, aliasCount);
                    } else {
                        processColumn(tableInfo, column, field.getAnnotation(Id.class), field.getAnnotation(Filterable.class), field.getName(), field.getGenericType(), false);
                    }
                } else {
                    JoinRelation[] joinRelations = field.getAnnotationsByType(JoinRelation.class);
                    if (joinRelations.length > 0) {
                        processJoinProperty(tableInfo, c, joinRelations, null, field.getAnnotation(LoadStrategy.class), field.getAnnotation(Filterable.class), field.getName(), field.getGenericType(), false, featureToJoinTableInfo, aliasCount);
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
                        processJoinProperty(tableInfo, c, new JoinRelation[0], column, readMethod.getAnnotation(LoadStrategy.class), readMethod.getAnnotation(Filterable.class), propertyDescriptor.getName(), readMethod.getGenericReturnType(), propertyDescriptor.getWriteMethod() == null, featureToJoinTableInfo, aliasCount);
                    } else {
                        processColumn(tableInfo, column, readMethod.getAnnotation(Id.class), readMethod.getAnnotation(Filterable.class), propertyDescriptor.getName(), readMethod.getGenericReturnType(), propertyDescriptor.getWriteMethod() == null);
                    }
                } else {
                    JoinRelation[] joinRelations = readMethod.getAnnotationsByType(JoinRelation.class);
                    if (joinRelations.length > 0) {
                        processJoinProperty(tableInfo, c, joinRelations, null, readMethod.getAnnotation(LoadStrategy.class), readMethod.getAnnotation(Filterable.class), propertyDescriptor.getName(), readMethod.getGenericReturnType(), propertyDescriptor.getWriteMethod() == null, featureToJoinTableInfo, aliasCount);
                    }
                }
            }

            if (c.isAnnotationPresent(EmbedParent.class)) {
                continue;
            }
            if (!c.isAnnotationPresent(JoinParent.class)) {
                break;
            }
            if (c.getGenericSuperclass() != null && c.getGenericSuperclass().isAnnotationPresent(TableRef.class)) {
                // TODO
                break;
            }
            inJoinParent = true;
        }
    }

    private void processColumn(TableInfo tableInfo, Column column, @Nullable Id id, Filterable filterable, String propertyName, GenericType propertyType, boolean readonly) {
        PropertyInfo propertyInfo = buildColumnPropertyInfo(tableInfo, column, id, filterable, "", propertyName, propertyType, readonly);
        tableInfo.getNameToPropertyInfo().put(propertyName, propertyInfo);
    }

    private PropertyInfo buildColumnPropertyInfo(TableInfo tableInfo, Column column, @Nullable Id id, Filterable filterable, String prefix, String propertyName, GenericType propertyType, boolean readonly) {
        PropertyInfo propertyInfo = buildPropertyInfo(prefix, propertyName, propertyType);
        propertyInfo.setOwnColumn(true);
        propertyInfo.setReadonly(readonly);
        propertyInfo.setJdbcType(column.jdbcType());
        propertyInfo.setJoinTableInfo(tableInfo.getJoinTableInfo());
        propertyInfo.setFilterableInfo(buildFilterableInfo(filterable));
        if (propertyInfo.getResultType() == ResultType.ASSOCIATION) {
            propertyInfo.putAll(collectColumnPropertyInfos(tableInfo, propertyInfo.getFullName(), GenericTypeFactory.build(propertyInfo.getJavaType()), readonly));
        } else if (propertyInfo.getResultType() == ResultType.COLLECTION && !configuration.getTypeHandlerRegistry().hasTypeHandler(propertyInfo.getOfType().getType())) {
            propertyInfo.putAll(collectColumnPropertyInfos(tableInfo, propertyInfo.getFullName(), GenericTypeFactory.build(propertyInfo.getOfType()), readonly));
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

    private Map<String, PropertyInfo> collectColumnPropertyInfos(TableInfo tableInfo, String prefix, @Nonnull GenericType javaType, boolean readonly) {
        Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();
        for (GenericType c = javaType; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            for (GenericField field : c.getDeclaredFields()) {
                if (nameToPropertyInfo.containsKey(field.getName())) {
                    continue;
                }
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    PropertyInfo propertyInfo = buildColumnPropertyInfo(tableInfo, column, field.getAnnotation(Id.class), field.getAnnotation(Filterable.class), prefix, field.getName(), field.getGenericType(), readonly);
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
                PropertyInfo propertyInfo = buildColumnPropertyInfo(tableInfo, column, readMethod.getAnnotation(Id.class), readMethod.getAnnotation(Filterable.class), prefix, propertyDescriptor.getName(), readMethod.getGenericReturnType(), readonly || propertyDescriptor.getWriteMethod() == null);
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

    private void processJoinProperty(TableInfo tableInfo, GenericType currentClass, JoinRelation[] joinRelations, @Nullable Column column, @Nullable LoadStrategy loadStrategy, Filterable filterable, String propertyName, GenericType propertyType, boolean readonly, Map<Set<JoinColumnFeature>, JoinTableInfo> featureToJoinTableInfo, AtomicInteger aliasCount) {
        PropertyInfo propertyInfo = buildPropertyInfo("", propertyName, propertyType);
        propertyInfo.setOwnColumn(false);
        propertyInfo.setReadonly(readonly);
        propertyInfo.setFilterableInfo(buildFilterableInfo(filterable));
        tableInfo.getNameToPropertyInfo().put(propertyName, propertyInfo);

        buildJoinTableInfos(tableInfo, currentClass, propertyInfo, joinRelations);
        mergeJoinTableInfos(tableInfo, propertyInfo, featureToJoinTableInfo, aliasCount);

        if (column != null) {
            propertyInfo.setColumnName(StringUtils.isNotBlank(column.name()) ? column.name() : StringUtils.camelToSnake(propertyName));
        } else {
            JoinRelation joinRelation = joinRelations[joinRelations.length - 1];
            if (joinRelation.table() != void.class) {
                String refName = StringUtils.isNotBlank(joinRelation.column()) ? joinRelation.column() : propertyName;
                PropertyInfo refPropertyInfo = deepGet(propertyInfo.getJoinTableInfo().getTableInfo(), refName);
                if (refPropertyInfo == null || !refPropertyInfo.isOwnColumn() || StringUtils.isBlank(refPropertyInfo.getColumnName())) {
                    throw new MybatisExtException("Column property '" + refName + "': not found in '" + propertyInfo.getJoinTableInfo().getTableInfo().getTableClass() + "'.");
                }
                propertyInfo.setColumnName(refPropertyInfo.getColumnName());
            }
        }
        if (loadStrategy != null) {
            propertyInfo.setLoadType(loadStrategy.value());
            if (propertyInfo.getResultType() == ResultType.RESULT && propertyInfo.getLoadType() != LoadType.JOIN) {
                propertyInfo.setResultType(ResultType.ASSOCIATION);
            }
        }

        if (propertyInfo.getColumnName() == null &&
                (propertyInfo.getResultType() == ResultType.ASSOCIATION || propertyInfo.getResultType() == ResultType.COLLECTION)) {
            propertyInfo.putAll(collectJoinTablePropertyInfos(propertyInfo.getJoinTableInfo(), propertyInfo.getJoinTableInfo().getTableInfo().getNameToPropertyInfo().values(), propertyInfo.getFullName()));
        }
    }

    private void buildJoinTableInfos(TableInfo rootTableInfo, GenericType currentClass, PropertyInfo propertyInfo, JoinRelation[] joinRelations) {
        JoinTableInfo lastJoinTableInfo = rootTableInfo.getJoinTableInfo();
        JoinTableInfo parentJoinTableInfo = rootTableInfo.getJoinTableInfo();
        GenericType tableClass = rootTableInfo.getTableClass();

        for (GenericType c = tableClass; currentClass.isAssignableFrom(c) && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            lastJoinTableInfo = parentJoinTableInfo;
            if (c.isAnnotationPresent(EmbedParent.class)) {
                continue;
            }
            if (!c.isAnnotationPresent(JoinParent.class) || c.getGenericSuperclass() == null || !c.getGenericSuperclass().isAnnotationPresent(Table.class)) {
                break;
            }
            JoinParent joinParent = c.getAnnotation(JoinParent.class);
            TableInfo tableInfo = getTableInfo(c.getGenericSuperclass());
            parentJoinTableInfo = new JoinTableInfo();
            parentJoinTableInfo.setTableInfo(tableInfo);
            parentJoinTableInfo.setAlias(joinParent.alias());

            for (JoinColumn joinColumn : joinParent.joinColumn()) {
                JoinColumnInfo joinColumnInfo = new JoinColumnInfo();
                joinColumnInfo.setLeftColumn(new PropertyInfo(joinColumn.leftColumn()));
                joinColumnInfo.setRightColumn(new PropertyInfo(joinColumn.rightColumn()));
                lastJoinTableInfo.getRightJoinTableInfos().put(joinColumnInfo, parentJoinTableInfo);
                parentJoinTableInfo.getLeftJoinTableInfos().put(joinColumnInfo, lastJoinTableInfo);
            }
        }

        Map<String, JoinTableInfo> aliasToJoinTableInfo = new HashMap<>();
        if (StringUtils.isNotBlank(lastJoinTableInfo.getAlias())) {
            aliasToJoinTableInfo.put(lastJoinTableInfo.getAlias(), lastJoinTableInfo);
        }
        if (parentJoinTableInfo != lastJoinTableInfo && StringUtils.isNotBlank(parentJoinTableInfo.getAlias())) {
            if (parentJoinTableInfo.getAlias().equals(lastJoinTableInfo.getAlias())) {
                throw new MybatisExtException("Duplicate table alias: " + parentJoinTableInfo.getAlias());
            }
            aliasToJoinTableInfo.put(parentJoinTableInfo.getAlias(), parentJoinTableInfo);
        }

        processJoinRelations(lastJoinTableInfo, propertyInfo, joinRelations, aliasToJoinTableInfo);
        checkJoinTableInfos(aliasToJoinTableInfo);
    }

    private void processJoinRelations(JoinTableInfo lastJoinTableInfo, PropertyInfo propertyInfo, JoinRelation[] joinRelations, Map<String, JoinTableInfo> aliasToJoinTableInfo) {
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
                joinColumnInfo.setLeftColumn(new PropertyInfo(joinColumn.leftColumn()));
                joinColumnInfo.setRightColumn(new PropertyInfo(joinColumn.rightColumn()));

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
        Set<JoinTableInfo> merged = new HashSet<>(queue);

        for (int i = 0; i < queue.size(); i++) {
            Set<JoinTableInfo> joinTableInfos = new HashSet<>(queue.get(i).getRightJoinTableInfos().values());
            for (JoinTableInfo joinTableInfo : joinTableInfos) {
                if (merged.contains(joinTableInfo) || !merged.containsAll(joinTableInfo.getLeftJoinTableInfos().values())) {
                    continue;
                }
                merged.add(joinTableInfo);
                queue.add(joinTableInfo);

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

    private TableInfo resolveTableInfoFromJoinRelation(PropertyInfo propertyInfo, JoinRelation joinRelation) {
        if (joinRelation.table() != void.class) {
            return getTableInfo(joinRelation.table());
        }
        if (propertyInfo.getOfType() != null) {
            return getTableInfo(propertyInfo.getOfType());
        }
        return getTableInfo(propertyInfo.getJavaType());
    }

    private PropertyInfo buildPropertyInfo(String prefix, String name, GenericType propertyType) {
        PropertyInfo propertyInfo = new PropertyInfo(prefix, name);
        if (Collection.class.isAssignableFrom(propertyType.getType())) {
            propertyInfo.setJavaType(propertyType);
            propertyInfo.setOfType(TypeArgumentResolver.resolveGenericType(propertyType, Collection.class, 0));
            propertyInfo.setResultType(ResultType.COLLECTION);
        } else if (propertyType.getType() == Optional.class) {
            propertyInfo.setJavaType(TypeArgumentResolver.resolveGenericType(propertyType, Collection.class, 0));
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

    private static Map<String, PropertyInfo> collectJoinTablePropertyInfos(JoinTableInfo joinTableInfo, Collection<PropertyInfo> propertyInfos, String prefix) {
        Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();
        for (PropertyInfo propertyInfo : propertyInfos) {
            if (!propertyInfo.isOwnColumn()) {
                continue;
            }
            PropertyInfo newPropertyInfo = new PropertyInfo(prefix, propertyInfo.getName());
            copyPropertyInfoProperties(newPropertyInfo, propertyInfo);
            newPropertyInfo.setJoinTableInfo(joinTableInfo);
            newPropertyInfo.setOwnColumn(false);
            newPropertyInfo.putAll(collectJoinTablePropertyInfos(joinTableInfo, propertyInfo.values(), propertyInfo.getFullName()));
            nameToPropertyInfo.put(propertyInfo.getName(), newPropertyInfo);
        }
        return nameToPropertyInfo;
    }

    public static void copyPropertyInfoProperties(PropertyInfo dest, PropertyInfo origin) {
        dest.setColumnName(origin.getColumnName());
        dest.setJoinTableInfo(origin.getJoinTableInfo());
        dest.setJavaType(origin.getJavaType());
        dest.setJdbcType(origin.getJdbcType());
        dest.setOwnColumn(origin.isOwnColumn());
        dest.setReadonly(origin.isReadonly());
        dest.setResultType(origin.getResultType());
        dest.setIdType(origin.getIdType());
        dest.setCustomIdGenerator(origin.getCustomIdGenerator());
        dest.setLoadType(origin.getLoadType());
        dest.setOfType(origin.getOfType());
        dest.setFilterableInfo(origin.getFilterableInfo());
    }

    private FilterableInfo buildFilterableInfo(@Nullable Filterable filterable) {
        if (filterable == null) {
            if (!extContext.isDefaultFilterable()) {
                return null;
            }
            FilterableInfo filterableInfo = new FilterableInfo();
            filterableInfo.setTest(IfTest.NotNull);
            filterableInfo.setOperator(CompareOperator.Equals);
            filterableInfo.setLogicalOperator(LogicalOperator.AND);
            return filterableInfo;
        }
        FilterableInfo filterableInfo = new FilterableInfo();
        filterableInfo.setTest(filterable.test());
        filterableInfo.setOperator(filterable.operator());
        filterableInfo.setLogicalOperator(filterable.logicalOperator());
        filterableInfo.setIgnorecase(filterable.ignorecase());
        filterableInfo.setNot(filterable.not());
        filterableInfo.setTestTemplate(filterable.testTemplate());
        filterableInfo.setExprTemplate(filterable.exprTemplate());
        filterableInfo.setSecondVariable(filterable.secondVariable());
        return filterableInfo;
    }

    private static JoinColumnFeature buildJoinColumnFeature(JoinColumnInfo joinColumnInfo, JoinTableInfo leftJoinTableInfo, JoinTableInfo righJoinTableInfo) {
        JoinColumnFeature joinColumnFeature = new JoinColumnFeature();
        joinColumnFeature.setLeftColumn(joinColumnInfo.getLeftColumn().getFullName());
        joinColumnFeature.setRightColumn(joinColumnInfo.getRightColumn().getFullName());
        joinColumnFeature.setLeftTable(leftJoinTableInfo);
        joinColumnFeature.setRightTable(righJoinTableInfo.getTableInfo());
        return joinColumnFeature;
    }
}
