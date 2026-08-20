package io.github.mybatisext.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;

import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.reflect.GenericTypeFactory;

class JoinGraphFactoryTest {

    private final Configuration configuration = new Configuration();

    @Test
    void cachesGraphsByUnderlyingTableType() {
        TableDef table = build(RootTable.class);
        TableDef tableRef = build(RootTableView.class);

        assertSame(JoinGraphFactory.buildJoinGraph(table), JoinGraphFactory.buildJoinGraph(tableRef));
    }

    @Test
    void prefersExplicitAliasesAndGeneratesUniqueFallbacks() {
        JoinGraph graph = new JoinGraph();

        JoinNode root = JoinGraphFactory.buildJoinNode(graph, build(RootTable.class));
        JoinNode conflicting = JoinGraphFactory.buildJoinNode(graph, build(ConflictingAliasTable.class));
        JoinNode generated = JoinGraphFactory.buildJoinNode(graph, build(UnaliasedTable.class));

        assertEquals("root", root.getAlias());
        assertEquals("t0", conflicting.getAlias());
        assertEquals("t1", generated.getAlias());
    }

    @Test
    void reusesAliasesForEqualJoinPathsOnly() {
        JoinGraph graph = new JoinGraph();
        JoinNode root = JoinGraphFactory.buildJoinNode(graph, build(PathRootTable.class));
        TableDef target = build(PathTargetTable.class);

        JoinNode first = JoinGraphFactory.deriveJoinNode(graph, Collections.singleton(path(root, "target_id", "id")), target, "target");
        JoinNode same = JoinGraphFactory.deriveJoinNode(graph, Collections.singleton(path(root, "target_id", "id")), target, "ignored");
        JoinNode different = JoinGraphFactory.deriveJoinNode(graph, Collections.singleton(path(root, "backup_id", "id")), target, "target");

        assertEquals("target", first.getAlias());
        assertEquals(first.getAlias(), same.getAlias());
        assertNotEquals(first.getAlias(), different.getAlias());
    }

    private TableDef build(Class<?> type) {
        return TableDefFactory.buildTableDef(GenericTypeFactory.build(type), configuration);
    }

    private JoinPath path(JoinNode left, String leftColumn, String rightColumn) {
        JoinPath path = new JoinPath();
        path.setLeftJoinNode(left);
        path.setLeftColumn(leftColumn);
        path.setRightColumn(rightColumn);
        return path;
    }

    @Table(alias = "root")
    static class RootTable {
        @Column
        private Long id;
    }

    @TableRef(RootTable.class)
    static class RootTableView {
    }

    @Table(alias = "root")
    static class ConflictingAliasTable {
        @Column
        private Long id;
    }

    @Table
    static class UnaliasedTable {
        @Column
        private Long id;
    }

    @Table(alias = "path_root")
    static class PathRootTable {
        @Column
        private Long id;
    }

    @Table
    static class PathTargetTable {
        @Column
        private Long id;
    }
}
