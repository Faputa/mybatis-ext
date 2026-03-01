package io.github.mybatisext.metadata;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinParent;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.util.StringUtils;

public class JoinGraphFactory {

    private static final Map<GenericType, JoinGraph> rootTableTypeToJoinGraph = new ConcurrentHashMap<>();

    public static JoinGraph buildJoinGraph(TableDef tableDef) {
        return buildJoinGraph(tableDef.getTableType());
    }

    private static JoinGraph buildJoinGraph(GenericType tableType) {
        GenericType superclass = tableType.getGenericSuperclass();
        if (superclass != null && superclass.getType() != Object.class) {
            if (tableType.isAnnotationPresent(JoinParent.class)) {
                return buildJoinGraph(superclass);
            }
        }
        return rootTableTypeToJoinGraph.computeIfAbsent(tableType, k -> new JoinGraph());
    }

    public static JoinNode buildJoinNode(JoinGraph joinGraph, TableDef tableDef, Configuration configuration) {
        return buildJoinNode(joinGraph, tableDef.getTableType(), configuration);
    }

    private static JoinNode buildJoinNode(JoinGraph joinGraph, GenericType tableType, Configuration configuration) {
        GenericType superclass = tableType.getGenericSuperclass();
        JoinParent joinParent = tableType.getAnnotation(JoinParent.class);
        if (superclass != null && superclass.getType() != Object.class && joinParent == null) {
            return buildJoinNode(joinGraph, superclass, configuration);
        }
        JoinNode joinNode = new JoinNode();
        joinNode.setTableType(tableType);
        Table table = tableType.getAnnotation(Table.class);
        resolveJoinNodeAlias(joinGraph, joinNode, table != null ? table.alias() : null);
        if (joinParent != null) {
            if (superclass == null || superclass.getType() == Object.class) {
                throw new MybatisExtException("@JoinParent on '" + tableType.getName() + "' requires a superclass.");
            }
            TableDef tableDef = TableDefFactory.buildTableDef(tableType, configuration);
            TableDef parentTableDef = TableDefFactory.buildTableDef(superclass, configuration);
            Set<JoinPath> leftJoinPaths = new HashSet<>();
            for (JoinColumn joinColumn : joinParent.joinColumn()) {
                leftJoinPaths.add(buildJoinPath(joinNode, tableDef, parentTableDef, joinColumn));
            }
            JoinNode redirect = buildJoinNode(joinGraph, superclass, configuration);
            JoinNode rightJoinNode = new JoinNode();
            rightJoinNode.setTableType(superclass);
            rightJoinNode.setLeftJoinPaths(leftJoinPaths);
            joinGraph.getJoinNodeToRedirect().put(rightJoinNode, redirect);
        }
        return joinNode;
    }

    private static JoinPath buildJoinPath(JoinNode leftJoinNode, TableDef leftTableDef, TableDef rigthTableDef, JoinColumn joinColumn) {
        JoinPath joinPath = new JoinPath();
        joinPath.setLeftJoinNode(leftJoinNode);
        joinPath.setLeftColumn(TableDefFactory.getOwnColumnName(leftTableDef, joinColumn.leftColumn()));
        joinPath.setRightColumn(TableDefFactory.getOwnColumnName(rigthTableDef, joinColumn.rightColumn()));
        return joinPath;
    }

    public static JoinNode deriveJoinNode(JoinGraph joinGraph, Set<JoinPath> joinPaths, TableDef tableDef, String defaultName) {
        JoinNode joinNode = new JoinNode();
        joinNode.setLeftJoinPaths(joinPaths);
        joinNode.setTableType(tableDef.getTableType());
        JoinNode redirect = joinGraph.getJoinNodeToRedirect().get(joinNode);
        if (redirect != null) {
            return redirect;
        }
        resolveJoinNodeAlias(joinGraph, joinNode, defaultName);
        return joinNode;
    }

    private static void resolveJoinNodeAlias(JoinGraph joinGraph, JoinNode joinNode, String defaultName) {
        String alias = joinGraph.getJoinNodeToAlias().get(joinNode);
        if (alias != null) {
            joinNode.setAlias(alias);
            return;
        }
        alias = buildAlias(joinGraph, defaultName);
        joinNode.setAlias(alias);
        joinGraph.getJoinNodeToAlias().put(joinNode, alias);
    }

    private static String buildAlias(JoinGraph joinGraph, String defaultName) {
        if (StringUtils.isNotBlank(defaultName)) {
            if (joinGraph.getAliasRegistry().add(defaultName)) {
                return defaultName;
            }
        }
        for (int i = 0;; i++) {
            String name = "t" + i;
            if (joinGraph.getAliasRegistry().add(name)) {
                return name;
            }
        }
    }
}
