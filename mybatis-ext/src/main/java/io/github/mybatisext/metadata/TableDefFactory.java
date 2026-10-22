package io.github.mybatisext.metadata;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

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
import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.diagnostic.DiagnosticLocation;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.model.JavaAnnotatedElement;
import io.github.mybatisext.model.JavaAnnotation;
import io.github.mybatisext.model.JavaBeanProperty;
import io.github.mybatisext.model.JavaField;
import io.github.mybatisext.model.JavaMethod;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.JavaTypes;
import io.github.mybatisext.model.TypePolicy;
import io.github.mybatisext.model.UnresolvedTypeException;
import io.github.mybatisext.reflect.ReflectionTypePolicy;
import io.github.mybatisext.util.StringUtils;

public class TableDefFactory {

    private final Map<JavaType, TableDef> classTypeToTableDef = new ConcurrentHashMap<>();

    private final TypePolicy typePolicy;

    public TableDefFactory(Configuration configuration) {
        this(new ReflectionTypePolicy(configuration));
    }

    public TableDefFactory(TypePolicy typePolicy) {
        this.typePolicy = typePolicy;
    }

    public TableDef buildTableDef(JavaType classType) {
        return buildTableDef(classType, null);
    }

    private TableDef buildTableDef(JavaType classType, Set<PropertyKey> propertyKeys) {
        TableDef _tableDef = classTypeToTableDef.get(classType);
        if (_tableDef != null) {
            return _tableDef;
        }
        JavaType tableType = resolveTableType(classType);
        if (tableType == null) {
            throw new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, "Table or TableRef annotation not found for type: " + classType.getName(), new DiagnosticLocation(classType, classType, null, null, null)));
        }
        TableName tableName = new TableName();
        {
            JavaAnnotation table = tableType.annotation(Table.class);
            tableName.setName(StringUtils.isNotBlank(table.value("name")) ? table.value("name") : StringUtils.camelToSnake(tableType.getSimpleName()));
            tableName.setSchema(table.value("schema"));
        }
        TableDef tableDef = new TableDef();
        tableDef.setClassType(classType);
        tableDef.setTableType(tableType);
        tableDef.setTableName(tableName);

        JavaAnnotation tableRef = null;
        JavaAnnotation table = null;
        if (classType.hasAnnotation(TableRef.class)) {
            tableRef = classType.annotation(TableRef.class);
        } else if (classType.hasAnnotation(Table.class)) {
            table = classType.annotation(Table.class);
        }
        if (propertyKeys == null) {
            propertyKeys = new HashSet<>();
        }
        tableDef.setNameToPropertyDef(buildNameToPropertyDef(classType, tableRef, table, true, true, propertyKeys));
        return classTypeToTableDef.computeIfAbsent(classType, k -> tableDef);
    }

    public static JavaType resolveTableType(JavaType classType) {
        Set<JavaType> types = new HashSet<>();
        while (classType != null && !JavaTypes.is(classType, Object.class.getName())) {
            if (!classType.isResolved()) {
                throw new UnresolvedTypeException(classType.getName()).at(new DiagnosticLocation(classType, classType, null, null, null));
            }
            if (classType.hasAnnotation(TableRef.class)) {
                classType = classType.annotation(TableRef.class).value("value");
            } else if (classType.hasAnnotation(Table.class)) {
                return classType;
            } else {
                classType = classType.getGenericSuperclass();
            }
            if (types.contains(classType)) {
                throw new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, "Circular @TableRef reference detected: " + classType.getName(), new DiagnosticLocation(classType, classType, TableRef.class, "value", null)));
            }
            types.add(classType);
        }
        return null;
    }

    private Map<String, PropertyDef> buildNameToPropertyDef(JavaType classType, JavaAnnotation tableRef, JavaAnnotation table, boolean ownColumn, boolean isCascade, Set<PropertyKey> propertyKeys) {
        JavaBeanProperty[] beanProperties = classType.getBeanProperties();
        Map<String, PropertyDef> nameToPropertyDef = new HashMap<>();

        while (classType != null && !JavaTypes.is(classType, Object.class.getName())) {
            if (!classType.isResolved()) {
                throw new UnresolvedTypeException(classType.getName()).at(new DiagnosticLocation(classType, classType, null, null, null));
            }
            if (classType.hasAnnotation(TableRef.class)) {
                tableRef = classType.annotation(TableRef.class);
                table = null;
            } else if (classType.hasAnnotation(Table.class)) {
                tableRef = null;
                table = classType.annotation(Table.class);
            }
            TableDef refTableDef = null;
            for (JavaField field : classType.getDeclaredFields()) {
                if (nameToPropertyDef.containsKey(field.getName())) {
                    continue;
                }
                if (table != null && field.hasAnnotation(Column.class)) {
                    PropertyDef propertyDef = buildPropertyDefForColumn(classType, tableRef, table, ownColumn, propertyKeys, field.getName(), field.getGenericType(), field, false);
                    nameToPropertyDef.put(field.getName(), propertyDef);
                } else if (tableRef != null && field.hasAnnotation(ColumnRef.class)) {
                    if (refTableDef == null) {
                        refTableDef = buildTableDef(tableRef.<JavaType>value("value"), recordPropertyKeys(propertyKeys, classType, field.getName(), field, ColumnRef.class));
                    }
                    PropertyDef propertyDef = buildPropertyDefForColumnRef(classType, tableRef, table, ownColumn, propertyKeys, refTableDef, field.getName(), field.getGenericType(), field, false);
                    nameToPropertyDef.put(field.getName(), propertyDef);
                } else if (isCascade && field.annotations(JoinRelation.class).length > 0) {
                    PropertyDef propertyDef = buildPropertyDefForJoinRelations(classType, tableRef, table, propertyKeys, field.getName(), field.getGenericType(), field, field.annotations(JoinRelation.class), false);
                    nameToPropertyDef.put(field.getName(), propertyDef);
                }
            }

            for (JavaBeanProperty propertyDescriptor : beanProperties) {
                if (nameToPropertyDef.containsKey(propertyDescriptor.getName())) {
                    continue;
                }
                JavaMethod readMethod = propertyDescriptor.getReadMethod();
                if (readMethod == null || !readMethod.getDeclaringType().equals(classType)) {
                    continue;
                }
                if (table != null && readMethod.hasAnnotation(Column.class)) {
                    PropertyDef propertyDef = buildPropertyDefForColumn(classType, tableRef, table, ownColumn, propertyKeys, propertyDescriptor.getName(), readMethod.getGenericReturnType(), readMethod, propertyDescriptor.isReadonly());
                    nameToPropertyDef.put(propertyDescriptor.getName(), propertyDef);
                } else if (tableRef != null && readMethod.hasAnnotation(ColumnRef.class)) {
                    if (refTableDef == null) {
                        refTableDef = buildTableDef(tableRef.<JavaType>value("value"), recordPropertyKeys(propertyKeys, classType, propertyDescriptor.getName(), readMethod, ColumnRef.class));
                    }
                    PropertyDef propertyDef = buildPropertyDefForColumnRef(classType, tableRef, table, ownColumn, propertyKeys, refTableDef, propertyDescriptor.getName(), readMethod.getGenericReturnType(), readMethod, propertyDescriptor.isReadonly());
                    nameToPropertyDef.put(propertyDescriptor.getName(), propertyDef);
                } else if (isCascade && readMethod.annotations(JoinRelation.class).length > 0) {
                    PropertyDef propertyDef = buildPropertyDefForJoinRelations(classType, tableRef, table, propertyKeys, propertyDescriptor.getName(), readMethod.getGenericReturnType(), readMethod, readMethod.annotations(JoinRelation.class), propertyDescriptor.isReadonly());
                    nameToPropertyDef.put(propertyDescriptor.getName(), propertyDef);
                }
            }

            if (classType.hasAnnotation(JoinParent.class)) {
                ownColumn = false;
            }
            classType = classType.getGenericSuperclass();
        }
        return nameToPropertyDef;
    }

    private PropertyDef buildPropertyDefForColumn(JavaType classType, JavaAnnotation tableRef, JavaAnnotation table, boolean ownColumn, Set<PropertyKey> propertyKeys, String propertyName, JavaType propertyType, JavaAnnotatedElement source, boolean readonly) {
        PropertyDef propertyDef = new PropertyDef();
        propertyDef.setSource(source);
        propertyDef.setSourceType(classType);
        propertyDef.setName(propertyName);
        propertyDef.setClassType(propertyType);
        propertyDef.setDeclaringType(classType);
        propertyDef.setOwnColumn(ownColumn);
        propertyDef.setReadonly(readonly);
        JavaAnnotation column = source.annotation(Column.class);
        propertyDef.setId(source.annotation(Id.class));
        propertyDef.setFilterable(source.annotation(Filterable.class));
        propertyDef.setJoinRelations(new JavaAnnotation[0]);
        propertyDef.setJdbcType(column.value("jdbcType") == JdbcType.UNDEFINED ? null : column.value("jdbcType"));
        DiagnosticLocation location = new DiagnosticLocation(classType, source, Column.class, null, propertyName);
        JavaType genericType = JavaTypes.unwrap(propertyType, location);
        if (typePolicy.isScalar(genericType, location)) {
            propertyDef.setColumnName(StringUtils.isNotBlank(column.value("name")) ? column.value("name") : StringUtils.camelToSnake(propertyName));
        } else {
            propertyDef.setNameToPropertyDef(buildNameToPropertyDef(genericType, tableRef, table, ownColumn, true, recordPropertyKeys(propertyKeys, classType, propertyName, source, Column.class)));
        }
        return propertyDef;
    }

    private PropertyDef buildPropertyDefForJoinRelations(JavaType classType, JavaAnnotation tableRef, JavaAnnotation table, Set<PropertyKey> propertyKeys, String propertyName, JavaType propertyType, JavaAnnotatedElement source, JavaAnnotation[] joinRelations, boolean readonly) {
        PropertyDef propertyDef = new PropertyDef();
        propertyDef.setSource(source);
        propertyDef.setSourceType(classType);
        propertyDef.setName(propertyName);
        propertyDef.setClassType(propertyType);
        propertyDef.setDeclaringType(classType);
        propertyDef.setOwnColumn(false);
        propertyDef.setReadonly(readonly);
        propertyDef.setJoinRelations(joinRelations);
        propertyDef.setFetch(source.annotation(Fetch.class));
        propertyDef.setCascade(source.annotation(Cascade.class));
        DiagnosticLocation location = new DiagnosticLocation(classType, source, JoinRelation.class, null, propertyName);
        JavaType genericType = JavaTypes.unwrap(propertyType, location);
        if (!typePolicy.isScalar(genericType, location)) {
            propertyDef.setNameToPropertyDef(buildNameToPropertyDef(genericType, tableRef, table, false, propertyDef.getCascade() != null && propertyDef.getCascade().<Boolean>value("value"), recordPropertyKeys(propertyKeys, classType, propertyName, source, JoinRelation.class)));
        }
        return propertyDef;
    }

    private PropertyDef buildPropertyDefForColumnRef(JavaType classType, JavaAnnotation tableRef, JavaAnnotation table, boolean ownColumn, Set<PropertyKey> propertyKeys, TableDef refTableDef, String propertyName, JavaType propertyType, JavaAnnotatedElement source, boolean readonly) {
        JavaAnnotation columnRef = source.annotation(ColumnRef.class);
        String originName = StringUtils.isNotBlank(columnRef.value("value")) ? columnRef.value("value") : propertyName;
        PropertyDef refPropertyDef = getPropertyDef(refTableDef, originName, new DiagnosticLocation(classType, source, ColumnRef.class, "value", originName).forPath(originName, StringUtils.isNotBlank(columnRef.value("value")) ? originName : null));
        PropertyDef propertyDef = new PropertyDef();
        propertyDef.setSource(source);
        propertyDef.setMappingSource(refPropertyDef.getMappingSource() != null ? refPropertyDef.getMappingSource() : refPropertyDef.getSource());
        propertyDef.setSourceType(classType);
        propertyDef.setName(propertyName);
        propertyDef.setClassType(propertyType);
        // 内嵌路径归属于外层属性所在的表，而非内嵌 Java 类型对应的表。
        String rootName = originName.contains(".") ? originName.substring(0, originName.indexOf('.')) : originName;
        propertyDef.setDeclaringType(refTableDef.getNameToPropertyDef().get(rootName).getDeclaringType());
        propertyDef.setJdbcType(refPropertyDef.getJdbcType());
        propertyDef.setOwnColumn(ownColumn && refPropertyDef.isOwnColumn());
        propertyDef.setReadonly(readonly);
        propertyDef.setColumnName(refPropertyDef.getColumnName());
        propertyDef.setJoinRelations(refPropertyDef.getJoinRelations());
        JavaAnnotation fetch = source.annotation(Fetch.class);
        JavaAnnotation filterable = source.annotation(Filterable.class);
        JavaAnnotation cascade = source.annotation(Cascade.class);
        propertyDef.setFetch(fetch != null ? fetch : refPropertyDef.getFetch());
        propertyDef.setFilterable(filterable != null ? filterable : refPropertyDef.getFilterable());
        propertyDef.setCascade(cascade != null ? cascade : refPropertyDef.getCascade());
        propertyDef.setId(refPropertyDef.getId());
        DiagnosticLocation location = new DiagnosticLocation(classType, source, ColumnRef.class, null, propertyName);
        JavaType genericType = JavaTypes.unwrap(propertyType, location);
        if (!typePolicy.isScalar(genericType, location)) {
            propertyDef.setNameToPropertyDef(buildNameToPropertyDef(genericType, tableRef, table, propertyDef.isOwnColumn(), propertyDef.getCascade() != null && propertyDef.getCascade().<Boolean>value("value"), recordPropertyKeys(propertyKeys, classType, propertyName, source, ColumnRef.class)));
        }
        return propertyDef;
    }

    private static Set<PropertyKey> recordPropertyKeys(Set<PropertyKey> propertyKeys, JavaType ownerType, String propertyName, JavaAnnotatedElement source, Class<? extends Annotation> annotationType) {
        PropertyKey propertyKey = new PropertyKey(ownerType, propertyName);
        if (propertyKeys.contains(propertyKey)) {
            throw new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, "Circular property reference detected: " + ownerType + "." + propertyName, new DiagnosticLocation(ownerType, source, annotationType, null, propertyName)));
        }
        Set<PropertyKey> newPropertyKeys = new HashSet<>(propertyKeys);
        newPropertyKeys.add(propertyKey);
        return newPropertyKeys;
    }

    public static PropertyDef getOwnSingleColumn(TableDef tableDef, String fullName) {
        return getOwnSingleColumn(tableDef, fullName, new DiagnosticLocation(tableDef.getClassType(), tableDef.getClassType(), null, null, fullName).forPath(fullName, fullName));
    }

    static PropertyDef getOwnSingleColumn(TableDef tableDef, String fullName, DiagnosticLocation location) {
        PropertyDef propertyDef = getPropertyDef(tableDef, fullName, location);
        if (!propertyDef.isOwnColumn() || StringUtils.isBlank(propertyDef.getColumnName())) {
            throw new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, "Property '" + fullName + "' is not an own column or has no column name", location));
        }
        return propertyDef;
    }

    private static PropertyDef getPropertyDef(TableDef tableDef, String fullName, DiagnosticLocation location) {
        return PropertyPath.resolve(tableDef.getNameToPropertyDef(), fullName, tableDef.getClassType(), PropertyDef::getNameToPropertyDef, location, null);
    }
}
