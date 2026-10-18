package io.github.mybatisext.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.annotation.JoinParent;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.annotation.TestMode;
import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.diagnostic.DiagnosticLocation;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.CompareOperator;
import io.github.mybatisext.jpa.LogicalOperator;
import io.github.mybatisext.model.JavaAnnotation;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.JavaTypes;
import io.github.mybatisext.model.TypePolicy;
import io.github.mybatisext.reflect.ReflectionTypePolicy;
import io.github.mybatisext.util.StringUtils;

public class TableInfoFactory {

    private final Map<JavaType, TableInfo> classTypeToTableInfo = new ConcurrentHashMap<>();

    private final TypePolicy typePolicy;
    private final ExtContext extContext;
    private final TableDefFactory tableDefFactory;
    private final JoinGraphFactory joinGraphFactory = new JoinGraphFactory();
    private boolean validateRuntimeTypes;

    public TableInfoFactory(Configuration configuration, ExtContext extContext) {
        this(new ReflectionTypePolicy(configuration), extContext);
        this.validateRuntimeTypes = true;
    }

    public TableInfoFactory(TypePolicy typePolicy, ExtContext extContext) {
        this.typePolicy = typePolicy;
        this.extContext = extContext;
        this.tableDefFactory = new TableDefFactory(typePolicy);
    }

    public TableInfo buildTableInfo(JavaType classType) {
        TableInfo _tableInfo = classTypeToTableInfo.get(classType);
        if (_tableInfo != null) {
            return _tableInfo;
        }
        TableDef tableDef = tableDefFactory.buildTableDef(classType);
        TableInfo tableInfo = buildTableInfo(tableDef);
        return classTypeToTableInfo.computeIfAbsent(classType, k -> tableInfo);
    }

    public TableInfo buildTableInfo(TableDef tableDef) {
        JoinGraph joinGraph = joinGraphFactory.buildJoinGraph(tableDef);
        JoinNode joinNode = JoinGraphFactory.buildJoinNode(joinGraph, tableDef);
        JoinTableInfo joinTableInfo = new JoinTableInfo();
        joinTableInfo.setTableDef(tableDef);
        joinTableInfo.setJoinNode(joinNode);

        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableDef.getTableName());
        tableInfo.setClassType(tableDef.getClassType());
        tableInfo.setJoinGraph(joinGraph);
        tableInfo.setJoinTableInfo(joinTableInfo);
        tableInfo.getAliasToJoinTableInfo().put(joinNode.getAlias(), joinTableInfo);

        processNameToPropertyDef(tableDef.getClassType(), tableDef.getNameToPropertyDef(), tableInfo.getNameToPropertyInfo(), "", "", joinTableInfo, tableInfo);
        return tableInfo;
    }

    private void processNameToPropertyDef(JavaType classType, Map<String, PropertyDef> nameToPropertyDef, Map<String, PropertyInfo> nameToPropertyInfo, String propertyPrefix, String ownPropertyPrefix, JoinTableInfo joinTableInfo, TableInfo tableInfo) {
        TableName tableName = null;
        String alias = null;
        if (classType.hasAnnotation(TableRef.class)) {
            tableName = tableDefFactory.buildTableDef(classType).getTableName();
            JavaAnnotation tableRef = classType.annotation(TableRef.class);
            alias = tableRef.value("alias");
            processNameToPropertyDef(tableRef.value("value"), nameToPropertyDef, nameToPropertyInfo, propertyPrefix, ownPropertyPrefix, joinTableInfo, tableInfo);
        } else if (classType.hasAnnotation(Table.class)) {
            tableName = tableDefFactory.buildTableDef(classType).getTableName();
            alias = classType.annotation(Table.class).value("alias");
        }
        if (tableName != null) {
            if (!tableName.equals(joinTableInfo.getTableDef().getTableName())) {
                throw new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, "Table name mismatch: expected '" + joinTableInfo.getTableDef().getTableName() + "', but got '" + tableName + "'", new DiagnosticLocation(classType, classType, classType.hasAnnotation(TableRef.class) ? TableRef.class : Table.class, classType.hasAnnotation(TableRef.class) ? "value" : "name", propertyPrefix)));
            }
        }
        Map<String, JoinTableInfo> localAliasToJoinTableInfo = new HashMap<>();
        if (StringUtils.isNotBlank(alias)) {
            localAliasToJoinTableInfo.put(alias, joinTableInfo);
        }

        JoinTableInfo parentJoinTableInfo = null;
        JavaType superclass = classType.getGenericSuperclass();
        JavaAnnotation joinParent = classType.annotation(JoinParent.class);
        if (joinParent != null) {
            if (superclass == null || superclass.is(Object.class)) {
                throw new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, "@JoinParent on '" + classType.getName() + "' requires a superclass.", new DiagnosticLocation(classType, classType, JoinParent.class, null, propertyPrefix)));
            }
            TableDef parentTableDef = tableDefFactory.buildTableDef(superclass);
            Set<JoinColumnInfo> joinColumnInfos = new HashSet<>();
            for (JavaAnnotation joinColumn : joinParent.<JavaAnnotation[]>value("joinColumn")) {
                joinColumnInfos.add(buildJoinColumnInfo(joinTableInfo, parentTableDef, ownPropertyPrefix + joinColumn.value("leftColumn"), joinColumn.value("rightColumn"), new DiagnosticLocation(classType, classType, JoinParent.class, "joinColumn", propertyPrefix), joinColumn.value("leftColumn")));
            }
            Set<JoinPath> joinPaths = buildJoinPaths(joinColumnInfos);
            JoinNode parentJoinNode = JoinGraphFactory.deriveJoinNode(tableInfo.getJoinGraph(), joinPaths, parentTableDef, joinParent.value("alias"));
            parentJoinTableInfo = buildJoinTableInfo(tableInfo, parentJoinNode, parentTableDef, joinColumnInfos);
            if (StringUtils.isNotBlank(joinParent.value("alias"))) {
                if (localAliasToJoinTableInfo.containsKey(joinParent.<String>value("alias"))) {
                    throw new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, "Duplicate alias '" + joinParent.value("alias") + "' in @JoinParent", new DiagnosticLocation(classType, classType, JoinParent.class, "alias", propertyPrefix)));
                }
                localAliasToJoinTableInfo.put(joinParent.value("alias"), parentJoinTableInfo);
            }
        }

        for (PropertyDef propertyDef : nameToPropertyDef.values()) {
            if (!propertyDef.getDeclaringType().equals(classType)) {
                continue;
            }
            if (nameToPropertyInfo.containsKey(propertyDef.getName())) {
                continue;
            }
            PropertyInfo propertyInfo = buildPropertyInfo(propertyDef, tableInfo, propertyPrefix, ownPropertyPrefix, joinTableInfo, new HashMap<>(localAliasToJoinTableInfo));
            nameToPropertyInfo.put(propertyInfo.getName(), propertyInfo);
        }
        if (superclass == null || superclass.is(Object.class)) {
            return;
        }
        if (parentJoinTableInfo != null) {
            joinTableInfo = parentJoinTableInfo;
            ownPropertyPrefix = "";
        }
        processNameToPropertyDef(superclass, nameToPropertyDef, nameToPropertyInfo, propertyPrefix, ownPropertyPrefix, joinTableInfo, tableInfo);
    }

    private PropertyInfo buildPropertyInfo(PropertyDef propertyDef, TableInfo tableInfo, String propertyPrefix, String ownPropertyPrefix, JoinTableInfo joinTableInfo, Map<String, JoinTableInfo> localAliasToJoinTableInfo) {
        PropertyInfo propertyInfo = new PropertyInfo();
        propertyInfo.setSource(propertyDef.getSource());
        propertyInfo.setMappingSource(propertyDef.getMappingSource());
        propertyInfo.setName(propertyDef.getName());
        propertyInfo.setFullName(propertyPrefix + propertyDef.getName());
        propertyInfo.setColumnName(propertyDef.getColumnName());
        propertyInfo.setOwnColumn(propertyDef.isOwnColumn());
        propertyInfo.setReadonly(propertyDef.isReadonly());
        propertyInfo.setFilterableInfo(buildFilterableInfo(propertyDef.getFilterable()));
        propertyInfo.setJdbcType(propertyDef.getJdbcType());

        JavaType propertyType = propertyDef.getClassType();
        if (propertyType.isAssignableTo(Collection.class)) {
            propertyInfo.setJavaType(propertyType);
            propertyInfo.setOfType(JavaTypes.unwrap(propertyType));
            propertyInfo.setPropertyType(PropertyType.COLLECTION);
        } else if (propertyType.is(Optional.class)) {
            propertyInfo.setJavaType(JavaTypes.unwrap(propertyType));
        } else if (propertyType.getTypeParameters().length == 0) {
            propertyInfo.setJavaType(propertyType);
        }
        if (propertyInfo.getJavaType() == null) {
            throw new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, "Unsupported property type: " + propertyType, new DiagnosticLocation(propertyDef.getSourceType(), propertyDef.getSource(), null, null, propertyInfo.getFullName())));
        }
        if (propertyInfo.getPropertyType() == null) {
            if (typePolicy.isScalar(propertyInfo.getJavaType(), new DiagnosticLocation(propertyDef.getSourceType(), propertyDef.getSource(), null, null, propertyInfo.getFullName()))) {
                propertyInfo.setPropertyType(PropertyType.RESULT);
            } else {
                propertyInfo.setPropertyType(PropertyType.ASSOCIATION);
            }
        }

        JavaAnnotation id = propertyDef.getId();
        if (id != null) {
            if (id.value("idType") == IdType.CUSTOM) {
                if (validateRuntimeTypes) {
                    ReflectionTypePolicy.validateIdGenerator(id.value("customIdGenerator"), new DiagnosticLocation(propertyDef.getSourceType(), propertyDef.getSource(), Id.class, "customIdGenerator", propertyInfo.getFullName()));
                }
                propertyInfo.setCustomIdGenerator(id.value("customIdGenerator"));
            }
            propertyInfo.setIdType(id.value("idType"));
            propertyInfo.setPropertyType(PropertyType.ID);
        }

        JavaAnnotation fetch = propertyDef.getFetch();
        if (fetch != null) {
            propertyInfo.setFetchType(fetch.value("value"));
            if (propertyInfo.getPropertyType() == PropertyType.RESULT) {
                propertyInfo.setPropertyType(PropertyType.ASSOCIATION);
            }
        }

        JoinTableInfo originJoinTableInfo = joinTableInfo;
        JavaAnnotation[] joinRelations = propertyDef.getJoinRelations();
        for (JavaAnnotation joinRelation : joinRelations) {
            JavaType rightTableType = resolveJoinRelationTableType(propertyInfo, joinRelation);
            TableDef rightTableDef = tableDefFactory.buildTableDef(rightTableType);
            Set<JoinColumnInfo> joinColumnInfos = new HashSet<>();
            for (JavaAnnotation joinColumn : joinRelation.<JavaAnnotation[]>value("joinColumn")) {
                JoinTableInfo leftJoinTableInfo;
                if (StringUtils.isNotBlank(joinColumn.value("leftTableAlias"))) {
                    leftJoinTableInfo = localAliasToJoinTableInfo.get(joinColumn.<String>value("leftTableAlias"));
                    if (leftJoinTableInfo == null) {
                        throw new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, "Unknown left table alias '" + joinColumn.value("leftTableAlias") + "' in @JoinColumn", new DiagnosticLocation(propertyDef.getSourceType(), propertyDef.getSource(), JoinRelation.class, "joinColumn.leftTableAlias", propertyInfo.getFullName())));
                    }
                } else {
                    leftJoinTableInfo = joinTableInfo;
                }
                joinColumnInfos.add(buildJoinColumnInfo(leftJoinTableInfo, rightTableDef, originJoinTableInfo == leftJoinTableInfo ? ownPropertyPrefix + joinColumn.value("leftColumn") : joinColumn.value("leftColumn"), joinColumn.value("rightColumn"), new DiagnosticLocation(propertyDef.getSourceType(), propertyDef.getSource(), JoinRelation.class, "joinColumn", propertyInfo.getFullName()), joinColumn.value("leftColumn")));
            }
            Set<JoinPath> joinPaths = buildJoinPaths(joinColumnInfos);
            JoinNode rightJoinNode = JoinGraphFactory.deriveJoinNode(tableInfo.getJoinGraph(), joinPaths, rightTableDef, joinRelation.value("tableAlias"));
            JoinTableInfo rightJoinTableInfo = buildJoinTableInfo(tableInfo, rightJoinNode, rightTableDef, joinColumnInfos);
            if (StringUtils.isNotBlank(joinRelation.value("tableAlias"))) {
                if (localAliasToJoinTableInfo.containsKey(joinRelation.<String>value("tableAlias"))) {
                    throw new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, "Duplicate table alias '" + joinRelation.value("tableAlias") + "' in @JoinRelation", new DiagnosticLocation(propertyDef.getSourceType(), propertyDef.getSource(), JoinRelation.class, "tableAlias", propertyInfo.getFullName())));
                }
                localAliasToJoinTableInfo.put(joinRelation.value("tableAlias"), rightJoinTableInfo);
            }
            joinTableInfo = rightJoinTableInfo;
        }
        propertyInfo.setJoinTableInfo(joinTableInfo);

        if (joinRelations.length > 0) {
            JavaAnnotation joinRelation = joinRelations[joinRelations.length - 1];
            if (!joinRelation.<JavaType>value("table").is(void.class)) {
                String refName = StringUtils.isNotBlank(joinRelation.value("column")) ? joinRelation.value("column") : propertyInfo.getName();
                PropertyDef refPropertyDef = TableDefFactory.getOwnSingleColumn(joinTableInfo.getTableDef(), refName, new DiagnosticLocation(propertyDef.getSourceType(), propertyDef.getSource(), JoinRelation.class, "column", refName).forPath(refName, StringUtils.isNotBlank(joinRelation.value("column")) ? refName : null));
                propertyInfo.setColumnName(refPropertyDef.getColumnName());
                propertyInfo.setJdbcType(refPropertyDef.getJdbcType());
                propertyInfo.setMappingSource(refPropertyDef.getMappingSource() != null ? refPropertyDef.getMappingSource() : refPropertyDef.getSource());
            } else {
                propertyInfo.setMappingSource(resolveJoinRelationTableType(propertyInfo, joinRelation));
            }
            ownPropertyPrefix = "";
        } else {
            ownPropertyPrefix += propertyDef.getName() + ".";
        }

        if (propertyDef.getNameToPropertyDef() != null) {
            JavaType genericType = JavaTypes.unwrap(propertyDef.getClassType());
            processNameToPropertyDef(genericType, propertyDef.getNameToPropertyDef(), propertyInfo.getNameToPropertyInfo(), propertyInfo.getFullName() + ".", ownPropertyPrefix, joinTableInfo, tableInfo);
        }

        return propertyInfo;
    }

    private static JoinTableInfo buildJoinTableInfo(TableInfo tableInfo, JoinNode joinNode, TableDef tableDef, Set<JoinColumnInfo> joinColumnInfos) {
        JoinTableInfo parentJoinTableInfo = tableInfo.getAliasToJoinTableInfo().get(joinNode.getAlias());
        if (parentJoinTableInfo != null) {
            return parentJoinTableInfo;
        }
        parentJoinTableInfo = new JoinTableInfo();
        parentJoinTableInfo.setTableDef(tableDef);
        parentJoinTableInfo.setLeftJoinColumnInfos(joinColumnInfos);
        parentJoinTableInfo.setJoinNode(joinNode);
        tableInfo.getAliasToJoinTableInfo().put(joinNode.getAlias(), parentJoinTableInfo);
        return parentJoinTableInfo;
    }

    private FilterableInfo buildFilterableInfo(@Nullable JavaAnnotation filterable) {
        if (filterable == null) {
            if (!extContext.isDefaultFilterable()) {
                return null;
            }
            FilterableInfo filterableInfo = new FilterableInfo();
            filterableInfo.setTestMode(TestMode.NotNull);
            filterableInfo.setOperator(CompareOperator.Equals);
            filterableInfo.setLogicalOperator(LogicalOperator.AND);
            return filterableInfo;
        }
        return new FilterableInfo(filterable);
    }

    private static JavaType resolveJoinRelationTableType(PropertyInfo propertyInfo, JavaAnnotation joinRelation) {
        if (!joinRelation.<JavaType>value("table").is(void.class)) {
            return joinRelation.value("table");
        }
        if (propertyInfo.getOfType() != null) {
            return propertyInfo.getOfType();
        }
        return propertyInfo.getJavaType();
    }

    private static Set<JoinPath> buildJoinPaths(Set<JoinColumnInfo> joinColumnInfos) {
        Set<JoinPath> joinPaths = new HashSet<>();
        for (JoinColumnInfo joinColumnInfo : joinColumnInfos) {
            JoinPath joinPath = new JoinPath();
            joinPath.setLeftJoinNode(joinColumnInfo.getLeftJoinTableInfo().getJoinNode());
            joinPath.setLeftColumn(joinColumnInfo.getLeftColumn().getColumnName());
            joinPath.setRightColumn(joinColumnInfo.getRightColumn().getColumnName());
            joinPaths.add(joinPath);
        }
        return joinPaths;
    }

    private static JoinColumnInfo buildJoinColumnInfo(JoinTableInfo leftJoinTableInfo, TableDef rightTableDef, String leftColumn, String rightColumn, DiagnosticLocation location, String leftSourceText) {
        JoinColumnInfo joinColumnInfo = new JoinColumnInfo();
        joinColumnInfo.setLeftJoinTableInfo(leftJoinTableInfo);
        joinColumnInfo.setLeftFullName(leftColumn);
        joinColumnInfo.setRightFullName(rightColumn);
        joinColumnInfo.setLeftColumn(TableDefFactory.getOwnSingleColumn(leftJoinTableInfo.getTableDef(), leftColumn, location.member("joinColumn.leftColumn").forPath(leftColumn, leftSourceText)));
        joinColumnInfo.setRightColumn(TableDefFactory.getOwnSingleColumn(rightTableDef, rightColumn, location.member("joinColumn.rightColumn").forPath(rightColumn, rightColumn)));
        return joinColumnInfo;
    }

    public static PropertyInfo getPropertyInfo(TableInfo tableInfo, String fullName) {
        return PropertyPath.resolve(tableInfo.getNameToPropertyInfo(), fullName, tableInfo.getClassType(), PropertyInfo::getNameToPropertyInfo,
                new DiagnosticLocation(tableInfo.getClassType(), tableInfo.getClassType(), null, null, fullName).forPath(fullName, fullName), null);
    }

    public static boolean isSameTableType(JavaType left, JavaType right) {
        JavaType leftTableType = TableDefFactory.resolveTableType(left);
        if (leftTableType == null) {
            return false;
        }
        JavaType rightTableType = TableDefFactory.resolveTableType(right);
        if (rightTableType == null) {
            return false;
        }
        return leftTableType.equals(rightTableType);
    }
}
