package io.github.mybatisext.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.ColumnRef;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.model.TypePolicy;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.reflect.ReflectionTypePolicy;
import io.github.mybatisext.statement.NestedSelectHelper;

class MetadataCacheIsolationTest {

    @Test
    void isolatesTypeHandlersAndJoinGraphsWithSharedExtContext() {
        ExtContext context = new ExtContext();
        Configuration embeddedConfiguration = new Configuration();
        Configuration scalarConfiguration = new Configuration();
        scalarConfiguration.getTypeHandlerRegistry().register(Value.class, ObjectTypeHandler.class);
        TableInfoFactory embeddedFactory = new TableInfoFactory(embeddedConfiguration, context);
        TableInfoFactory scalarFactory = new TableInfoFactory(scalarConfiguration, context);
        GenericType type = GenericTypeFactory.build(Record.class);

        TableInfo embedded = embeddedFactory.buildTableInfo(type);
        TableInfo scalar = scalarFactory.buildTableInfo(type);
        assertSame(embedded, embeddedFactory.buildTableInfo(type));
        assertSame(scalar, scalarFactory.buildTableInfo(type));
        assertNotSame(embedded.getJoinGraph(), scalar.getJoinGraph());
        assertNotSame(embedded.getJoinTableInfo().getTableDef(), scalar.getJoinTableInfo().getTableDef());
        assertNull(embedded.getNameToPropertyInfo().get("value").getColumnName());
        assertEquals("code", embedded.getNameToPropertyInfo().get("value").getNameToPropertyInfo().get("code").getColumnName());
        assertEquals("value", scalar.getNameToPropertyInfo().get("value").getColumnName());
    }

    @Test
    void repeatedFailedBuildsReuseAliasesAndPreserveCachedMetadata() {
        TableInfoFactory factory = new TableInfoFactory(new Configuration(), new ExtContext());
        TableInfo root = factory.buildTableInfo(GenericTypeFactory.build(Root.class));
        TableInfo good = factory.buildTableInfo(GenericTypeFactory.build(GoodView.class));
        JoinNode goodNode = good.get("label").getJoinTableInfo().getJoinNode();
        String goodAlias = goodNode.getAlias();
        JoinGraph graph = root.getJoinGraph();
        Map<JoinNode, String> nodesBefore = new HashMap<>(graph.getJoinNodeToAlias());
        Set<String> aliasesBefore = new HashSet<>(graph.getAliasRegistry());
        assertThrows(MybatisExtException.class, () -> factory.buildTableInfo(GenericTypeFactory.build(BrokenView.class)));
        for (Map.Entry<JoinNode, String> entry : nodesBefore.entrySet()) {
            assertEquals(entry.getValue(), graph.getJoinNodeToAlias().get(entry.getKey()));
        }
        assertTrue(graph.getAliasRegistry().containsAll(aliasesBefore));

        Map<JoinNode, String> nodesAfterFailure = new HashMap<>(graph.getJoinNodeToAlias());
        Set<String> aliasesAfterFailure = new HashSet<>(graph.getAliasRegistry());
        assertThrows(MybatisExtException.class, () -> factory.buildTableInfo(GenericTypeFactory.build(BrokenView.class)));
        assertEquals(nodesAfterFailure, graph.getJoinNodeToAlias());
        assertEquals(aliasesAfterFailure, graph.getAliasRegistry());
        assertSame(root, factory.buildTableInfo(GenericTypeFactory.build(Root.class)));
        assertSame(good, factory.buildTableInfo(GenericTypeFactory.build(GoodView.class)));
        assertEquals(goodAlias, goodNode.getAlias());
        assertSame(good.get("label").getJoinTableInfo(), good.getAliasToJoinTableInfo().get(goodAlias));
    }

    @Test
    void failureBeforeAnySuccessfulBuildLeavesReusableJoinPaths() {
        TableInfoFactory factory = new TableInfoFactory(new Configuration(), new ExtContext());
        assertThrows(MybatisExtException.class, () -> factory.buildTableInfo(GenericTypeFactory.build(BrokenView.class)));
        TableInfo good = factory.buildTableInfo(GenericTypeFactory.build(GoodView.class));
        JoinGraph graph = good.getJoinGraph();
        Map<JoinNode, String> nodesBefore = new HashMap<>(graph.getJoinNodeToAlias());
        Set<String> aliasesBefore = new HashSet<>(graph.getAliasRegistry());

        TableInfo equivalent = factory.buildTableInfo(GenericTypeFactory.build(EquivalentView.class));
        assertSame(graph, equivalent.getJoinGraph());
        assertEquals(good.get("label").getJoinTableInfo().getAlias(), equivalent.get("label").getJoinTableInfo().getAlias());
        assertEquals(nodesBefore, graph.getJoinNodeToAlias());
        assertEquals(aliasesBefore, graph.getAliasRegistry());
        assertEquals(graph.getJoinNodeToAlias().size(), graph.getAliasRegistry().size());
    }

    @Test
    void equivalentSuccessfulProjectionsReuseAliasesInEitherBuildOrder() {
        TableInfoFactory forward = new TableInfoFactory(new Configuration(), new ExtContext());
        TableInfo first = forward.buildTableInfo(GenericTypeFactory.build(GoodView.class));
        TableInfo second = forward.buildTableInfo(GenericTypeFactory.build(EquivalentView.class));
        TableInfoFactory reverse = new TableInfoFactory(new Configuration(), new ExtContext());
        TableInfo secondFirst = reverse.buildTableInfo(GenericTypeFactory.build(EquivalentView.class));
        TableInfo firstLast = reverse.buildTableInfo(GenericTypeFactory.build(GoodView.class));
        assertSame(first.getJoinGraph(), second.getJoinGraph());
        assertEquals(first.get("label").getJoinTableInfo().getJoinNode().getAlias(), firstLast.get("label").getJoinTableInfo().getJoinNode().getAlias());
        assertEquals(second.get("label").getJoinTableInfo().getJoinNode().getAlias(), secondFirst.get("label").getJoinTableInfo().getJoinNode().getAlias());
    }

    @Test
    void concurrentProjectionsPublishUniqueAliasesAndReuseEquivalentPaths() throws Exception {
        Configuration configuration = new Configuration();
        TableDefFactory definitions = new TableDefFactory(configuration);
        TableDef firstDefinition = definitions.buildTableDef(GenericTypeFactory.build(GoodView.class));
        TableDef secondDefinition = definitions.buildTableDef(GenericTypeFactory.build(OtherView.class));
        CyclicBarrier barrier = new CyclicBarrier(2);
        ThreadLocal<Boolean> entered = ThreadLocal.withInitial(() -> false);
        ReflectionTypePolicy reflection = new ReflectionTypePolicy(configuration);
        TypePolicy policy = type -> {
            if (!entered.get()) {
                entered.set(true);
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new AssertionError("Metadata builds must run concurrently", e);
                }
            }
            return reflection.classify(type);
        };
        TableInfoFactory factory = new TableInfoFactory(policy, new ExtContext());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<TableInfo> firstBuild = executor.submit(() -> factory.buildTableInfo(firstDefinition));
            Future<TableInfo> secondBuild = executor.submit(() -> factory.buildTableInfo(secondDefinition));
            TableInfo first = firstBuild.get(20, TimeUnit.SECONDS);
            TableInfo second = secondBuild.get(20, TimeUnit.SECONDS);
            JoinNode firstNode = first.get("label").getJoinTableInfo().getJoinNode();
            JoinNode secondNode = second.get("label").getJoinTableInfo().getJoinNode();
            JoinGraph graph = first.getJoinGraph();
            assertSame(graph, second.getJoinGraph());
            assertNotEquals(firstNode.getAlias(), secondNode.getAlias());
            assertEquals(firstNode.getAlias(), graph.getJoinNodeToAlias().get(firstNode));
            assertEquals(secondNode.getAlias(), graph.getJoinNodeToAlias().get(secondNode));
            assertEquals(graph.getJoinNodeToAlias().size(), graph.getAliasRegistry().size());
            assertSame(first.get("label").getJoinTableInfo(), first.getAliasToJoinTableInfo().get(firstNode.getAlias()));
            assertSame(second.get("label").getJoinTableInfo(), second.getAliasToJoinTableInfo().get(secondNode.getAlias()));
            JoinTableInfo next = second.get("nextLabel").getJoinTableInfo();
            assertSame(second.get("label").getJoinTableInfo(), next.getLeftJoinColumnInfos().iterator().next().getLeftJoinTableInfo());
            String sql = NestedSelectHelper.buildExistSubSelect(second.get("nextLabel"), "1 = 1");
            assertTrue(sql.contains(next.getAlias() + ".id = " + secondNode.getAlias() + ".id"), sql);
            assertTrue(sql.contains(secondNode.getAlias() + ".id = r.target_id"), sql);

            Set<String> aliasesBefore = new HashSet<>(graph.getAliasRegistry());
            TableInfo equivalent = executor.submit(() -> factory.buildTableInfo(GenericTypeFactory.build(EquivalentView.class))).get(20, TimeUnit.SECONDS);
            assertEquals(firstNode.getAlias(), equivalent.get("label").getJoinTableInfo().getJoinNode().getAlias());
            assertEquals(aliasesBefore, graph.getAliasRegistry());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentBuildsOfSameTypeReturnSameCachedMetadata() throws Exception {
        TableInfoFactory factory = new TableInfoFactory(new Configuration(), new ExtContext());
        GenericType type = GenericTypeFactory.build(GoodView.class);
        int workers = 8;
        CyclicBarrier barrier = new CyclicBarrier(workers);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<TableInfo>> builds = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                builds.add(executor.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return factory.buildTableInfo(type);
                }));
            }
            TableInfo expected = builds.get(0).get(20, TimeUnit.SECONDS);
            for (Future<TableInfo> build : builds) {
                assertSame(expected, build.get(20, TimeUnit.SECONDS));
            }
            assertSame(expected, factory.buildTableInfo(type));
            assertEquals(expected.getJoinGraph().getJoinNodeToAlias().size(), expected.getJoinGraph().getAliasRegistry().size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Table(alias = "r")
    static class Root {

        @Column
        Long id;
        @Column
        Long targetId;
        @Column
        Long backupId;
    }

    @Table
    static class Target {

        @Column
        Long id;
        @Column
        String name;
    }

    @TableRef(Root.class)
    static class BrokenView {

        @ColumnRef("targetId")
        Long targetId;
        @JoinRelation(table = Target.class, tableAlias = "reserved", column = "name", joinColumn = @JoinColumn(leftColumn = "targetId", rightColumn = "id"))
        @JoinRelation(table = Target.class, tableAlias = "reserved", column = "name", joinColumn = @JoinColumn(leftColumn = "backupId", rightColumn = "id"))
        String label;
    }

    @TableRef(Root.class)
    static class GoodView {

        @ColumnRef("backupId")
        Long backupId;
        @JoinRelation(table = Target.class, tableAlias = "reserved", column = "name", joinColumn = @JoinColumn(leftColumn = "backupId", rightColumn = "id"))
        String label;
    }

    @TableRef(Root.class)
    static class EquivalentView {

        @ColumnRef("backupId")
        Long backupId;
        @JoinRelation(table = Target.class, tableAlias = "reserved", column = "name", joinColumn = @JoinColumn(leftColumn = "backupId", rightColumn = "id"))
        String label;
    }

    @TableRef(Root.class)
    static class OtherView {

        @ColumnRef("targetId")
        Long targetId;
        @JoinRelation(table = Target.class, tableAlias = "reserved", column = "name", joinColumn = @JoinColumn(leftColumn = "targetId", rightColumn = "id"))
        String label;

        @JoinRelation(table = Target.class, tableAlias = "reserved", joinColumn = @JoinColumn(leftColumn = "targetId", rightColumn = "id"))
        @JoinRelation(table = Target.class, tableAlias = "next", column = "name", joinColumn = @JoinColumn(leftTableAlias = "reserved", leftColumn = "id", rightColumn = "id"))
        String nextLabel;
    }

    @Table
    static class Record {

        @Column
        private Value value;
    }

    static class Value {

        @Column
        private String code;
    }
}
