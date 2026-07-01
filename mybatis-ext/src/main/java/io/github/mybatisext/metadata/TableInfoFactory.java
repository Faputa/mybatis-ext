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
import io.github.mybatisext.annotation.Fetch;
import io.github.mybatisext.annotation.Filterable;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinParent;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.annotation.TestMode;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.CompareOperator;
import io.github.mybatisext.jpa.LogicalOperator;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.util.StringUtils;
import io.github.mybatisext.util.TypeUtils;

public class TableInfoFactory {

    private static final Map<GenericType, TableInfo> classTypeToTableInfo = new ConcurrentHashMap<>();

    public static TableInfo buildTableInfo(GenericType classType, Configuration configuration, ExtContext extContext) {
        TableInfo _tableInfo = classTypeToTableInfo.get(classType);
        if (_tableInfo != null) {
            return _tableInfo;
        }
        TableDef tableDef = TableDefFactory.buildTableDef(classType, configuration);
        TableInfo tableInfo = buildTableInfo(tableDef, configuration, extContext);
        return classTypeToTableInfo.computeIfAbsent(classType, k -> tableInfo);
    }

    public static TableInfo buildTableInfo(TableDef tableDef, Configuration configuration, ExtContext extContext) {
        JoinGraph joinGraph = JoinGraphFactory.buildJoinGraph(tableDef);
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

        processNameToPropertyDef(tableDef.getClassType(), tableDef.getNameToPropertyDef(), tableInfo.getNameToPropertyInfo(), "", "", joinTableInfo, tableInfo, configuration, extContext);
        return tableInfo;
    }

    private static void processNameToPropertyDef(GenericType classType, Map<String, PropertyDef> nameToPropertyDef, Map<String, PropertyInfo> nameToPropertyInfo, String propertyPrefix, String ownPropertyPrefix, JoinTableInfo joinTableInfo, TableInfo tableInfo, Configuration configuration, ExtContext extContext) {
        for (;;) {
            TableName tableName = null;
            String alias = null;
            if (classType.isAnnotationPresent(TableRef.class)) {
                tableName = TableDefFactory.buildTableDef(classType, configuration).getTableName();
                TableRef tableRef = classType.getAnnotation(TableRef.class);
                alias = tableRef.alias();
                processNameToPropertyDef(GenericTypeFactory.build(tableRef.value()), nameToPropertyDef, nameToPropertyInfo, propertyPrefix, ownPropertyPrefix, joinTableInfo, tableInfo, configuration, extContext);
            } else if (classType.isAnnotationPresent(Table.class)) {
                tableName = TableDefFactory.buildTableDef(classType, configuration).getTableName();
                alias = classType.getAnnotation(Table.class).alias();
            }
            if (tableName != null) {
                if (!tableName.equals(joinTableInfo.getTableDef().getTableName())) {
                    throw new MybatisExtException("Table name mismatch: expected '" + joinTableInfo.getTableDef().getTableName() + "', but got '" + tableName + "'");
                }
            }
            Map<String, JoinTableInfo> localAliasToJoinTableInfo = new HashMap<>();
            if (StringUtils.isNotBlank(alias)) {
                localAliasToJoinTableInfo.put(alias, joinTableInfo);
            }

            JoinTableInfo parentJoinTableInfo = null;
            GenericType superclass = classType.getGenericSuperclass();
            JoinParent joinParent = classType.getAnnotation(JoinParent.class);
            if (joinParent != null) {
                if (superclass == null || superclass.getType() == Object.class) {
                    throw new MybatisExtException("@JoinParent on '" + classType.getName() + "' requires a superclass.");
                }
                TableDef parentTableDef = TableDefFactory.buildTableDef(superclass, configuration);
                Set<JoinColumnInfo> joinColumnInfos = new HashSet<>();
                for (JoinColumn joinColumn : joinParent.joinColumn()) {
                    joinColumnInfos.add(buildJoinColumnInfo(joinTableInfo, parentTableDef, ownPropertyPrefix + joinColumn.leftColumn(), joinColumn.rightColumn()));
                }
                Set<JoinPath> joinPaths = buildJoinPaths(joinColumnInfos);
                JoinNode parentJoinNode = JoinGraphFactory.deriveJoinNode(tableInfo.getJoinGraph(), joinPaths, parentTableDef, joinParent.alias());
                parentJoinTableInfo = buildJoinTableInfo(tableInfo, parentJoinNode, parentTableDef, joinColumnInfos);
                if (StringUtils.isNotBlank(joinParent.alias())) {
                    if (localAliasToJoinTableInfo.containsKey(joinParent.alias())) {
                        throw new MybatisExtException("Duplicate alias '" + joinParent.alias() + "' in @JoinParent");
                    }
                    localAliasToJoinTableInfo.put(joinParent.alias(), parentJoinTableInfo);
                }
            }

            for (PropertyDef propertyDef : nameToPropertyDef.values()) {
                if (!propertyDef.getDeclaringType().equals(classType)) {
                    continue;
                }
                if (nameToPropertyInfo.containsKey(propertyDef.getName())) {
                    continue;
                }
                PropertyInfo propertyInfo = buildPropertyInfo(propertyDef, tableInfo, propertyPrefix, ownPropertyPrefix, configuration, extContext, joinTableInfo, new HashMap<>(localAliasToJoinTableInfo));
                nameToPropertyInfo.put(propertyInfo.getName(), propertyInfo);
            }
            if (superclass == null || superclass.getType() == Object.class) {
                break;
            }
            if (parentJoinTableInfo != null) {
                joinTableInfo = parentJoinTableInfo;
                ownPropertyPrefix = "";
            }
            classType = superclass;
        }
    }

    private static PropertyInfo buildPropertyInfo(PropertyDef propertyDef, TableInfo tableInfo, String propertyPrefix, String ownPropertyPrefix, Configuration configuration, ExtContext extContext, JoinTableInfo joinTableInfo, Map<String, JoinTableInfo> localAliasToJoinTableInfo) {
        PropertyInfo propertyInfo = new PropertyInfo();
        propertyInfo.setName(propertyDef.getName());
        propertyInfo.setFullName(propertyPrefix + propertyDef.getName());
        propertyInfo.setColumnName(propertyDef.getColumnName());
        propertyInfo.setOwnColumn(propertyDef.isOwnColumn());
        propertyInfo.setReadonly(propertyDef.isReadonly());
        propertyInfo.setFilterableInfo(buildFilterableInfo(propertyDef.getFilterable(), extContext));
        propertyInfo.setJdbcType(propertyDef.getJdbcType());

        GenericType propertyType = propertyDef.getClassType();
        if (Collection.class.isAssignableFrom(propertyType.getType())) {
            propertyInfo.setJavaType(propertyType);
            propertyInfo.setOfType(TypeUtils.unwrapToGenericType(propertyType));
            propertyInfo.setPropertyType(PropertyType.COLLECTION);
        } else if (propertyType.getType() == Optional.class) {
            propertyInfo.setJavaType(TypeUtils.unwrapToGenericType(propertyType));
        } else if (propertyType.getTypeParameters().length == 0) {
            propertyInfo.setJavaType(propertyType);
        }
        if (propertyInfo.getJavaType() == null) {
            throw new MybatisExtException("Unsupported property type: " + propertyType);
        }
        if (propertyInfo.getPropertyType() == null) {
            if (configuration.getTypeHandlerRegistry().hasTypeHandler(propertyInfo.getJavaType().getType())) {
                propertyInfo.setPropertyType(PropertyType.RESULT);
            } else {
                propertyInfo.setPropertyType(PropertyType.ASSOCIATION);
            }
        }

        Id id = propertyDef.getId();
        if (id != null) {
            if (id.idType() == IdType.CUSTOM) {
                try {
                    id.customIdGenerator().newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new MybatisExtException("customIdGenerator cannot be instantiated", e);
                }
                propertyInfo.setCustomIdGenerator(id.customIdGenerator());
            }
            propertyInfo.setIdType(id.idType());
            propertyInfo.setPropertyType(PropertyType.ID);
        }

        Fetch fetch = propertyDef.getFetch();
        if (fetch != null) {
            propertyInfo.setFetchType(fetch.value());
            if (propertyInfo.getPropertyType() == PropertyType.RESULT) {
                propertyInfo.setPropertyType(PropertyType.ASSOCIATION);
            }
        }

        JoinTableInfo originJoinTableInfo = joinTableInfo;
        JoinRelation[] joinRelations = propertyDef.getJoinRelations();
        for (JoinRelation joinRelation : joinRelations) {
            GenericType rightTableType = resolveJoinRelationTableType(propertyInfo, joinRelation);
            TableDef rightTableDef = TableDefFactory.buildTableDef(rightTableType, configuration);
            Set<JoinColumnInfo> joinColumnInfos = new HashSet<>();
            for (JoinColumn joinColumn : joinRelation.joinColumn()) {
                JoinTableInfo leftJoinTableInfo;
                if (StringUtils.isNotBlank(joinColumn.leftTableAlias())) {
                    leftJoinTableInfo = localAliasToJoinTableInfo.get(joinColumn.leftTableAlias());
                    if (leftJoinTableInfo == null) {
                        throw new MybatisExtException("Unknown left table alias '" + joinColumn.leftTableAlias() + "' in @JoinColumn");
                    }
                } else {
                    leftJoinTableInfo = joinTableInfo;
                }
                joinColumnInfos.add(buildJoinColumnInfo(leftJoinTableInfo, rightTableDef, originJoinTableInfo == leftJoinTableInfo ? ownPropertyPrefix + joinColumn.leftColumn() : joinColumn.leftColumn(), joinColumn.rightColumn()));
            }
            Set<JoinPath> joinPaths = buildJoinPaths(joinColumnInfos);
            JoinNode rightJoinNode = JoinGraphFactory.deriveJoinNode(tableInfo.getJoinGraph(), joinPaths, rightTableDef, joinRelation.tableAlias());
            JoinTableInfo rightJoinTableInfo = buildJoinTableInfo(tableInfo, rightJoinNode, rightTableDef, joinColumnInfos);
            if (StringUtils.isNotBlank(joinRelation.tableAlias())) {
                if (localAliasToJoinTableInfo.containsKey(joinRelation.tableAlias())) {
                    throw new MybatisExtException("Duplicate table alias '" + joinRelation.tableAlias() + "' in @JoinRelation");
                }
                localAliasToJoinTableInfo.put(joinRelation.tableAlias(), rightJoinTableInfo);
            }
            joinTableInfo = rightJoinTableInfo;
        }
        propertyInfo.setJoinTableInfo(joinTableInfo);

        if (joinRelations.length > 0) {
            JoinRelation joinRelation = joinRelations[joinRelations.length - 1];
            if (joinRelation.table() != void.class) {
                String refName = StringUtils.isNotBlank(joinRelation.column()) ? joinRelation.column() : propertyInfo.getName();
                PropertyDef refPropertyDef = TableDefFactory.getOwnSingleColumn(joinTableInfo.getTableDef(), refName);
                propertyInfo.setColumnName(refPropertyDef.getColumnName());
                propertyInfo.setJdbcType(refPropertyDef.getJdbcType());
            }
            ownPropertyPrefix = "";
        } else {
            ownPropertyPrefix += propertyDef.getName() + ".";
        }

        if (propertyDef.getNameToPropertyDef() != null) {
            GenericType genericType = TypeUtils.unwrapToGenericType(propertyDef.getClassType());
            processNameToPropertyDef(genericType, propertyDef.getNameToPropertyDef(), propertyInfo.getNameToPropertyInfo(), propertyInfo.getFullName() + ".", ownPropertyPrefix, joinTableInfo, tableInfo, configuration, extContext);
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

    private static FilterableInfo buildFilterableInfo(@Nullable Filterable filterable, ExtContext extContext) {
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

    private static GenericType resolveJoinRelationTableType(PropertyInfo propertyInfo, JoinRelation joinRelation) {
        if (joinRelation.table() != void.class) {
            return GenericTypeFactory.build(joinRelation.table());
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

    private static JoinColumnInfo buildJoinColumnInfo(JoinTableInfo leftJoinTableInfo, TableDef rightTableDef, String leftColumn, String rightColumn) {
        JoinColumnInfo joinColumnInfo = new JoinColumnInfo();
        joinColumnInfo.setLeftJoinTableInfo(leftJoinTableInfo);
        joinColumnInfo.setLeftFullName(leftColumn);
        joinColumnInfo.setRightFullName(rightColumn);
        joinColumnInfo.setLeftColumn(TableDefFactory.getOwnSingleColumn(leftJoinTableInfo.getTableDef(), leftColumn));
        joinColumnInfo.setRightColumn(TableDefFactory.getOwnSingleColumn(rightTableDef, rightColumn));
        return joinColumnInfo;
    }

    public static PropertyInfo getPropertyInfo(TableInfo tableInfo, String fullName) {
        Map<String, PropertyInfo> map = tableInfo.getNameToPropertyInfo();
        PropertyInfo propertyInfo = null;
        for (String key : fullName.split("\\.")) {
            if ((propertyInfo = map.get(key)) == null) {
                break;
            }
            map = propertyInfo.getNameToPropertyInfo();
        }
        if (propertyInfo == null) {
            throw new MybatisExtException("Property '" + fullName + "' not found in " + tableInfo.getClassType().getSimpleName());
        }
        return propertyInfo;
    }

    public static boolean isSameTableType(GenericType left, GenericType right) {
        GenericType leftTableType = TableDefFactory.resolveTableType(left);
        if (leftTableType == null) {
            return false;
        }
        GenericType rightTableType = TableDefFactory.resolveTableType(right);
        if (rightTableType == null) {
            return false;
        }
        return leftTableType.equals(rightTableType);
    }
}
