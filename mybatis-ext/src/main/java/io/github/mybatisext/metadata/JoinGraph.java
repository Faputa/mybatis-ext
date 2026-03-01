package io.github.mybatisext.metadata;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JoinGraph {

    private final Map<JoinNode, String> joinNodeToAlias = new ConcurrentHashMap<>();
    private final Map<JoinNode, JoinNode> joinNodeToRedirect = new ConcurrentHashMap<>();
    private final Set<String> aliasRegistry = ConcurrentHashMap.newKeySet();

    public Map<JoinNode, String> getJoinNodeToAlias() {
        return joinNodeToAlias;
    }

    public Map<JoinNode, JoinNode> getJoinNodeToRedirect() {
        return joinNodeToRedirect;
    }

    public Set<String> getAliasRegistry() {
        return aliasRegistry;
    }
}
