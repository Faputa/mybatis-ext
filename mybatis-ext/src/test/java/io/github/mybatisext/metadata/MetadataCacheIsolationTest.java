package io.github.mybatisext.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

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
    void failedBuildsDoNotPublishAliasesOrChangeCachedMetadata() {
        TableInfoFactory factory = new TableInfoFactory(new Configuration(), new ExtContext());
        TableInfo root = factory.buildTableInfo(GenericTypeFactory.build(Root.class));
        JoinGraph graph = root.getJoinGraph();
        Map<JoinNode, String> nodesBefore = new HashMap<>(graph.getJoinNodeToAlias());
        Set<String> aliasesBefore = new HashSet<>(graph.getAliasRegistry());
        assertThrows(MybatisExtException.class, () -> factory.buildTableInfo(GenericTypeFactory.build(BrokenView.class)));
        assertEquals(nodesBefore, graph.getJoinNodeToAlias());
        assertEquals(aliasesBefore, graph.getAliasRegistry());
        assertSame(root, factory.buildTableInfo(GenericTypeFactory.build(Root.class)));

        TableInfo good = factory.buildTableInfo(GenericTypeFactory.build(GoodView.class));
        TableInfo fresh = new TableInfoFactory(new Configuration(), new ExtContext()).buildTableInfo(GenericTypeFactory.build(GoodView.class));
        assertEquals("reserved", good.get("label").getJoinTableInfo().getJoinNode().getAlias());
        assertEquals(fresh.get("label").getJoinTableInfo().getJoinNode().getAlias(), good.get("label").getJoinTableInfo().getJoinNode().getAlias());
        assertSame(graph, good.getJoinGraph());
        Set<String> goodAliases = new HashSet<>(graph.getAliasRegistry());
        assertThrows(MybatisExtException.class, () -> factory.buildTableInfo(GenericTypeFactory.build(BrokenView.class)));
        assertEquals(goodAliases, graph.getAliasRegistry());
        assertSame(good, factory.buildTableInfo(GenericTypeFactory.build(GoodView.class)));
        assertTrue(graph.getAliasRegistry().contains("r"));
    }

    @Test
    void failureBeforeAnySuccessfulBuildDoesNotChangeLaterAliases() {
        TableInfoFactory factory = new TableInfoFactory(new Configuration(), new ExtContext());
        assertThrows(MybatisExtException.class, () -> factory.buildTableInfo(GenericTypeFactory.build(BrokenView.class)));
        TableInfo actual = factory.buildTableInfo(GenericTypeFactory.build(GoodView.class));
        TableInfo expected = new TableInfoFactory(new Configuration(), new ExtContext()).buildTableInfo(GenericTypeFactory.build(GoodView.class));
        assertEquals(expected.getJoinGraph().getAliasRegistry(), actual.getJoinGraph().getAliasRegistry());
        assertEquals(expected.get("label").getJoinTableInfo().getJoinNode().getAlias(), actual.get("label").getJoinTableInfo().getJoinNode().getAlias());
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
