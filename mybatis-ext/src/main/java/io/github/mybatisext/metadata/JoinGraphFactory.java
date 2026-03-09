package io.github.mybatisext.metadata;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.util.StringUtils;

public class JoinGraphFactory {

    private static final Map<GenericType, JoinGraph> tableTypeToJoinGraph = new ConcurrentHashMap<>();

    public static JoinGraph buildJoinGraph(TableDef tableDef) {
        GenericType tableType = tableDef.getTableType();
        return tableTypeToJoinGraph.computeIfAbsent(tableType, k -> new JoinGraph());
    }

    public static JoinNode buildJoinNode(JoinGraph joinGraph, TableDef tableDef) {
        GenericType tableType = tableDef.getTableType();
        JoinNode joinNode = new JoinNode();
        joinNode.setTableType(tableType);
        resolveJoinNodeAlias(joinGraph, joinNode, tableType.getAnnotation(Table.class).alias());
        return joinNode;
    }

    public static JoinNode deriveJoinNode(JoinGraph joinGraph, Set<JoinPath> joinPaths, TableDef tableDef, String defaultName) {
        JoinNode joinNode = new JoinNode();
        joinNode.setLeftJoinPaths(joinPaths);
        joinNode.setTableType(tableDef.getTableType());
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
