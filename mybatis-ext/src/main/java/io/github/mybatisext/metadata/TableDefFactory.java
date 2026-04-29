package io.github.mybatisext.metadata;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.Cascade;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.ColumnRef;
import io.github.mybatisext.annotation.Fetch;
import io.github.mybatisext.annotation.Filterable;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinParent;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.reflect.GenericField;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.util.StringUtils;
import io.github.mybatisext.util.TypeUtils;

public class TableDefFactory {

    private static final Map<GenericType, TableDef> classTypeToTableDef = new ConcurrentHashMap<>();

    public static TableDef buildTableDef(GenericType classType, Configuration configuration) {
        return buildTableDef(classType, configuration, null);
    }

    private static TableDef buildTableDef(GenericType classType, Configuration configuration, Set<PropertyKey> propertyKeys) {
        TableDef _tableDef = classTypeToTableDef.get(classType);
        if (_tableDef != null) {
            return _tableDef;
        }
        GenericType tableType = resolveTableType(classType);
        if (tableType == null) {
            throw new MybatisExtException("Table or TableRef annotation not found for type: " + classType.getName());
        }
        TableName tableName = new TableName();
        {
            Table table = tableType.getAnnotation(Table.class);
            tableName.setName(StringUtils.isNotBlank(table.name()) ? table.name() : StringUtils.camelToSnake(tableType.getSimpleName()));
            tableName.setSchema(table.schema());
        }
        TableDef tableDef = new TableDef();
        tableDef.setClassType(classType);
        tableDef.setTableType(tableType);
        tableDef.setTableName(tableName);

        TableRef tableRef = null;
        Table table = null;
        if (classType.isAnnotationPresent(TableRef.class)) {
            tableRef = classType.getAnnotation(TableRef.class);
        } else if (classType.isAnnotationPresent(Table.class)) {
            table = classType.getAnnotation(Table.class);
        }
        if (propertyKeys == null) {
            propertyKeys = new HashSet<>();
        }
        tableDef.setNameToPropertyDef(buildNameToPropertyDef(classType, tableRef, table, configuration, true, true, propertyKeys));
        return classTypeToTableDef.computeIfAbsent(classType, k -> tableDef);
    }

    public static GenericType resolveTableType(GenericType classType) {
        Set<GenericType> types = new HashSet<>();
        while (classType != null && classType.getType() != Object.class) {
            if (classType.isAnnotationPresent(TableRef.class)) {
                classType = GenericTypeFactory.build(classType.getAnnotation(TableRef.class).value());
            } else if (classType.isAnnotationPresent(Table.class)) {
                return classType;
            } else {
                classType = classType.getGenericSuperclass();
            }
            if (types.contains(classType)) {
                throw new MybatisExtException("Circular @TableRef reference detected: " + classType.getName());
            }
            types.add(classType);
        }
        return null;
    }

    private static Map<String, PropertyDef> buildNameToPropertyDef(GenericType classType, TableRef tableRef, Table table, Configuration configuration, boolean ownColumn, boolean isCascade, Set<PropertyKey> propertyKeys) {
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(classType.getType(), Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException e) {
            throw new MybatisExtException(e);
        }
        Map<Method, GenericMethod> methodMap = Arrays.stream(classType.getMethods()).collect(Collectors.toMap(GenericMethod::getMethod, v -> v));
        Map<String, PropertyDef> nameToPropertyDef = new HashMap<>();

        for (;;) {
            if (classType.isAnnotationPresent(TableRef.class)) {
                tableRef = classType.getAnnotation(TableRef.class);
                table = null;
            } else if (classType.isAnnotationPresent(Table.class)) {
                tableRef = null;
                table = classType.getAnnotation(Table.class);
            }
            TableDef refTableDef = null;
            for (GenericField field : classType.getDeclaredFields()) {
                if (nameToPropertyDef.containsKey(field.getName())) {
                    continue;
                }
                if (table != null && field.isAnnotationPresent(Column.class)) {
                    PropertyDef propertyDef = new PropertyDef();
                    propertyDef.setName(field.getName());
                    propertyDef.setClassType(field.getGenericType());
                    propertyDef.setDeclaringType(classType);
                    propertyDef.setOwnColumn(ownColumn);
                    propertyDef.setReadonly(false);
                    Column column = field.getAnnotation(Column.class);
                    propertyDef.setId(field.getAnnotation(Id.class));
                    propertyDef.setFilterable(field.getAnnotation(Filterable.class));
                    propertyDef.setJoinRelations(new JoinRelation[0]);
                    GenericType genericType = TypeUtils.unwrapToGenericType(field.getGenericType());
                    if (configuration.getTypeHandlerRegistry().hasTypeHandler(genericType.getType())) {
                        propertyDef.setColumnName(StringUtils.isNotBlank(column.name()) ? column.name() : StringUtils.camelToSnake(field.getName()));
                    } else {
                        propertyDef.setNameToPropertyDef(buildNameToPropertyDef(genericType, tableRef, table, configuration, ownColumn, true, recordPropertyKeys(propertyKeys, classType, field.getName())));
                    }
                    nameToPropertyDef.put(field.getName(), propertyDef);
                } else if (tableRef != null && field.isAnnotationPresent(ColumnRef.class)) {
                    if (refTableDef == null) {
                        refTableDef = buildTableDef(GenericTypeFactory.build(tableRef.value()), configuration, recordPropertyKeys(propertyKeys, classType, field.getName()));
                    }
                    ColumnRef columnRef = field.getAnnotation(ColumnRef.class);
                    String originName = StringUtils.isNotBlank(columnRef.value()) ? columnRef.value() : field.getName();
                    PropertyDef refPropertyDef = getPropertyDef(refTableDef, originName);
                    PropertyDef propertyDef = new PropertyDef();
                    propertyDef.setName(field.getName());
                    propertyDef.setClassType(field.getGenericType());
                    propertyDef.setDeclaringType(refPropertyDef.getDeclaringType());
                    propertyDef.setOwnColumn(refPropertyDef.isOwnColumn());
                    propertyDef.setReadonly(false);
                    propertyDef.setColumnName(refPropertyDef.getColumnName());
                    propertyDef.setJoinRelations(refPropertyDef.getJoinRelations());
                    Fetch fetch = field.getAnnotation(Fetch.class);
                    Filterable filterable = field.getAnnotation(Filterable.class);
                    Cascade cascade = field.getAnnotation(Cascade.class);
                    propertyDef.setFetch(fetch != null ? fetch : refPropertyDef.getFetch());
                    propertyDef.setFilterable(filterable != null ? filterable : refPropertyDef.getFilterable());
                    propertyDef.setCascade(cascade != null ? cascade : refPropertyDef.getCascade());
                    propertyDef.setId(refPropertyDef.getId());
                    GenericType genericType = TypeUtils.unwrapToGenericType(field.getGenericType());
                    if (!configuration.getTypeHandlerRegistry().hasTypeHandler(genericType.getType())) {
                        propertyDef.setNameToPropertyDef(buildNameToPropertyDef(genericType, tableRef, table, configuration, ownColumn, propertyDef.getCascade() != null && propertyDef.getCascade().value(), recordPropertyKeys(propertyKeys, classType, field.getName())));
                    }
                    nameToPropertyDef.put(field.getName(), propertyDef);
                } else if (isCascade && field.getAnnotationsByType(JoinRelation.class).length > 0) {
                    PropertyDef propertyDef = new PropertyDef();
                    propertyDef.setName(field.getName());
                    propertyDef.setClassType(field.getGenericType());
                    propertyDef.setDeclaringType(classType);
                    propertyDef.setOwnColumn(false);
                    propertyDef.setReadonly(false);
                    propertyDef.setJoinRelations(field.getAnnotationsByType(JoinRelation.class));
                    propertyDef.setFetch(field.getAnnotation(Fetch.class));
                    propertyDef.setCascade(field.getAnnotation(Cascade.class));
                    GenericType genericType = TypeUtils.unwrapToGenericType(field.getGenericType());
                    if (!configuration.getTypeHandlerRegistry().hasTypeHandler(genericType.getType())) {
                        propertyDef.setNameToPropertyDef(buildNameToPropertyDef(genericType, tableRef, table, configuration, ownColumn, propertyDef.getCascade() != null && propertyDef.getCascade().value(), recordPropertyKeys(propertyKeys, classType, field.getName())));
                    }
                    nameToPropertyDef.put(field.getName(), propertyDef);
                }
            }

            for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                if (nameToPropertyDef.containsKey(propertyDescriptor.getName())) {
                    continue;
                }
                GenericMethod readMethod = methodMap.get(propertyDescriptor.getReadMethod());
                if (readMethod == null || readMethod.getDeclaringClass() != classType.getType()) {
                    continue;
                }
                if (table != null && readMethod.isAnnotationPresent(Column.class)) {
                    PropertyDef propertyDef = new PropertyDef();
                    propertyDef.setName(propertyDescriptor.getName());
                    propertyDef.setClassType(readMethod.getGenericReturnType());
                    propertyDef.setDeclaringType(classType);
                    propertyDef.setOwnColumn(ownColumn);
                    propertyDef.setReadonly(propertyDescriptor.getWriteMethod() == null);
                    Column column = readMethod.getAnnotation(Column.class);
                    propertyDef.setId(readMethod.getAnnotation(Id.class));
                    propertyDef.setFilterable(readMethod.getAnnotation(Filterable.class));
                    propertyDef.setJoinRelations(new JoinRelation[0]);
                    GenericType genericType = TypeUtils.unwrapToGenericType(readMethod.getGenericReturnType());
                    if (configuration.getTypeHandlerRegistry().hasTypeHandler(genericType.getType())) {
                        propertyDef.setColumnName(StringUtils.isNotBlank(column.name()) ? column.name() : StringUtils.camelToSnake(propertyDescriptor.getName()));
                    } else {
                        propertyDef.setNameToPropertyDef(buildNameToPropertyDef(genericType, tableRef, table, configuration, false, true, recordPropertyKeys(propertyKeys, classType, propertyDescriptor.getName())));
                    }
                    nameToPropertyDef.put(propertyDescriptor.getName(), propertyDef);
                } else if (tableRef != null && readMethod.isAnnotationPresent(ColumnRef.class)) {
                    if (refTableDef == null) {
                        refTableDef = buildTableDef(GenericTypeFactory.build(tableRef.value()), configuration, recordPropertyKeys(propertyKeys, classType, propertyDescriptor.getName()));
                    }
                    ColumnRef columnRef = readMethod.getAnnotation(ColumnRef.class);
                    String originName = StringUtils.isNotBlank(columnRef.value()) ? columnRef.value() : propertyDescriptor.getName();
                    PropertyDef refPropertyDef = getPropertyDef(refTableDef, originName);
                    PropertyDef propertyDef = new PropertyDef();
                    propertyDef.setName(propertyDescriptor.getName());
                    propertyDef.setClassType(readMethod.getGenericReturnType());
                    propertyDef.setDeclaringType(refPropertyDef.getDeclaringType());
                    propertyDef.setOwnColumn(refPropertyDef.isOwnColumn());
                    propertyDef.setReadonly(propertyDescriptor.getWriteMethod() == null);
                    propertyDef.setColumnName(refPropertyDef.getColumnName());
                    propertyDef.setJoinRelations(refPropertyDef.getJoinRelations());
                    Fetch fetch = readMethod.getAnnotation(Fetch.class);
                    Filterable filterable = readMethod.getAnnotation(Filterable.class);
                    Cascade cascade = readMethod.getAnnotation(Cascade.class);
                    propertyDef.setFetch(fetch != null ? fetch : refPropertyDef.getFetch());
                    propertyDef.setFilterable(filterable != null ? filterable : refPropertyDef.getFilterable());
                    propertyDef.setCascade(cascade != null ? cascade : refPropertyDef.getCascade());
                    propertyDef.setId(refPropertyDef.getId());
                    GenericType genericType = TypeUtils.unwrapToGenericType(readMethod.getGenericReturnType());
                    if (!configuration.getTypeHandlerRegistry().hasTypeHandler(genericType.getType())) {
                        propertyDef.setNameToPropertyDef(buildNameToPropertyDef(genericType, tableRef, table, configuration, false, propertyDef.getCascade() != null && propertyDef.getCascade().value(), recordPropertyKeys(propertyKeys, classType, propertyDescriptor.getName())));
                    }
                    nameToPropertyDef.put(propertyDescriptor.getName(), propertyDef);
                } else if (isCascade && readMethod.getAnnotationsByType(JoinRelation.class).length > 0) {
                    PropertyDef propertyDef = new PropertyDef();
                    propertyDef.setName(propertyDescriptor.getName());
                    propertyDef.setClassType(readMethod.getGenericReturnType());
                    propertyDef.setDeclaringType(classType);
                    propertyDef.setOwnColumn(false);
                    propertyDef.setReadonly(propertyDescriptor.getWriteMethod() == null);
                    propertyDef.setJoinRelations(readMethod.getAnnotationsByType(JoinRelation.class));
                    propertyDef.setFetch(readMethod.getAnnotation(Fetch.class));
                    GenericType genericType = TypeUtils.unwrapToGenericType(readMethod.getGenericReturnType());
                    if (!configuration.getTypeHandlerRegistry().hasTypeHandler(genericType.getType())) {
                        propertyDef.setNameToPropertyDef(buildNameToPropertyDef(genericType, tableRef, table, configuration, false, propertyDef.getCascade() != null && propertyDef.getCascade().value(), recordPropertyKeys(propertyKeys, classType, propertyDescriptor.getName())));
                    }
                    nameToPropertyDef.put(propertyDescriptor.getName(), propertyDef);
                }
            }

            GenericType superclass = classType.getGenericSuperclass();
            if (superclass == null || superclass.getType() == Object.class) {
                break;
            }
            if (classType.isAnnotationPresent(JoinParent.class)) {
                ownColumn = false;
            }
            classType = superclass;
        }
        return nameToPropertyDef;
    }

    private static Set<PropertyKey> recordPropertyKeys(Set<PropertyKey> propertyKeys, GenericType ownerType, String propertyName) {
        PropertyKey propertyKey = new PropertyKey(ownerType, propertyName);
        if (propertyKeys.contains(propertyKey)) {
            throw new MybatisExtException("Circular property reference detected: " + ownerType + "." + propertyName);
        }
        Set<PropertyKey> newPropertyKeys = new HashSet<>(propertyKeys);
        newPropertyKeys.add(propertyKey);
        return newPropertyKeys;
    }

    public static String getOwnColumnName(TableDef tableDef, String fullName) {
        PropertyDef propertyDef = getPropertyDef(tableDef, fullName);
        if (!propertyDef.isOwnColumn() || StringUtils.isBlank(propertyDef.getColumnName())) {
            throw new MybatisExtException("Property '" + fullName + "' is not an own column or has no column name");
        }
        return propertyDef.getColumnName();
    }

    private static PropertyDef getPropertyDef(TableDef tableDef, String fullName) {
        Map<String, PropertyDef> nameToPropertyDef = tableDef.getNameToPropertyDef();
        String[] ss = fullName.split("\\.");
        PropertyDef propertyDef = null;
        for (String s : ss) {
            propertyDef = nameToPropertyDef.get(s);
            if (propertyDef == null || propertyDef.getNameToPropertyDef() == null) {
                break;
            }
            nameToPropertyDef = propertyDef.getNameToPropertyDef();
        }
        if (propertyDef == null) {
            throw new MybatisExtException("Property '" + fullName + "' not found in " + tableDef.getClassType().getSimpleName());
        }
        return propertyDef;
    }
}
