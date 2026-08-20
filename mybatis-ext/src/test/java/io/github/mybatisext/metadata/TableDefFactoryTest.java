package io.github.mybatisext.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.ibatis.mapping.FetchType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.annotation.Cascade;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.ColumnRef;
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
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

class TableDefFactoryTest {

    private final Configuration configuration = new Configuration();

    @Test
    void buildsTableColumnsEmbeddedPropertiesAndRelations() {
        TableDef tableDef = build(Record.class);

        assertEquals(Record.class, tableDef.getClassType().getType());
        assertEquals(Record.class, tableDef.getTableType().getType());
        assertEquals("record", tableDef.getTableName().getName());
        assertEquals("metadata", tableDef.getTableName().getSchema());
        assertEquals(5, tableDef.getNameToPropertyDef().size());

        PropertyDef id = property(tableDef, "id");
        assertEquals("record_id", id.getColumnName());
        assertEquals(JdbcType.BIGINT, id.getJdbcType());
        assertEquals(IdType.UUID, id.getId().idType());
        assertTrue(id.isOwnColumn());
        assertFalse(id.isReadonly());

        PropertyDef name = property(tableDef, "name");
        assertEquals("name", name.getColumnName());
        assertEquals(TestMode.NotEmpty, name.getFilterable().test());
        assertEquals(CompareOperator.Like, name.getFilterable().operator());

        PropertyDef embedded = property(tableDef, "embedded");
        assertNull(embedded.getColumnName());
        assertEquals("code_value", embedded.getNameToPropertyDef().get("code").getColumnName());
        assertTrue(embedded.getNameToPropertyDef().get("code").isOwnColumn());

        PropertyDef targetName = property(tableDef, "targetName");
        assertFalse(targetName.isOwnColumn());
        assertEquals(1, targetName.getJoinRelations().length);
        assertEquals(FetchType.LAZY, targetName.getFetch().value());
        assertTrue(targetName.getCascade().value());

        PropertyDef computed = property(tableDef, "computed");
        assertEquals("computed", computed.getColumnName());
        assertTrue(computed.isReadonly());
    }

    @Test
    void columnRefCopiesMetadataAndAppliesLocalOverrides() {
        TableDef tableDef = build(RecordView.class);

        assertEquals(RecordView.class, tableDef.getClassType().getType());
        assertEquals(Record.class, tableDef.getTableType().getType());
        assertEquals("metadata.record", tableDef.getTableName().toString());

        PropertyDef id = property(tableDef, "id");
        assertEquals("record_id", id.getColumnName());
        assertEquals(JdbcType.BIGINT, id.getJdbcType());
        assertEquals(IdType.UUID, id.getId().idType());
        assertEquals(Record.class, id.getDeclaringType().getType());

        PropertyDef displayName = property(tableDef, "displayName");
        assertEquals("name", displayName.getColumnName());
        assertEquals(CompareOperator.Equals, displayName.getFilterable().operator());

        PropertyDef code = property(tableDef, "code");
        assertEquals("code_value", code.getColumnName());
        assertTrue(code.isOwnColumn());

        PropertyDef eagerTargetName = property(tableDef, "eagerTargetName");
        assertEquals(1, eagerTargetName.getJoinRelations().length);
        assertEquals(FetchType.EAGER, eagerTargetName.getFetch().value());
        assertFalse(eagerTargetName.getCascade().value());

        assertTrue(property(tableDef, "readonlyName").isReadonly());
    }

    @Test
    void joinParentMarksInheritedColumnsAsNonOwn() {
        TableDef tableDef = build(ChildRecord.class);

        assertTrue(property(tableDef, "childId").isOwnColumn());
        assertFalse(property(tableDef, "parentId").isOwnColumn());
    }

    @Test
    void cascadeControlsNestedRelationExpansion() {
        TableDef tableDef = build(RelationOwner.class);

        PropertyDef target = property(tableDef, "target");
        assertNotNull(target.getNameToPropertyDef().get("id"));
        assertNull(target.getNameToPropertyDef().get("relatedName"));

        PropertyDef cascadedTarget = property(tableDef, "cascadedTarget");
        assertNotNull(cascadedTarget.getNameToPropertyDef().get("id"));
        assertNotNull(cascadedTarget.getNameToPropertyDef().get("relatedName"));
    }

    @Test
    void resolvesTableTypesAndCachesDefinitions() {
        GenericType recordType = GenericTypeFactory.build(Record.class);

        assertEquals(recordType, TableDefFactory.resolveTableType(GenericTypeFactory.build(PlainRecordSubclass.class)));
        assertEquals(recordType, TableDefFactory.resolveTableType(GenericTypeFactory.build(RecordView.class)));
        assertNull(TableDefFactory.resolveTableType(GenericTypeFactory.build(UnmappedType.class)));
        assertEquals("target_record", build(TargetRecord.class).getTableName().getName());
        assertSame(build(Record.class), build(Record.class));
    }

    @Test
    void findsOnlyOwnLeafColumnsByNestedPath() {
        TableDef tableDef = build(Record.class);

        assertSame(property(tableDef, "id"), TableDefFactory.getOwnSingleColumn(tableDef, "id"));
        assertEquals("code_value", TableDefFactory.getOwnSingleColumn(tableDef, "embedded.code").getColumnName());

        assertMessage("is not an own column", () -> TableDefFactory.getOwnSingleColumn(tableDef, "embedded"));
        assertMessage("is not an own column", () -> TableDefFactory.getOwnSingleColumn(tableDef, "targetName"));
        assertMessage("not found", () -> TableDefFactory.getOwnSingleColumn(tableDef, "missing"));
    }

    @Test
    void rejectsMissingTableMetadataAndUnknownColumnReferences() {
        assertMessage("Table or TableRef annotation not found", () -> build(UnmappedType.class));
        assertMessage("Property 'missing' not found", () -> build(UnknownColumnRef.class));
    }

    @Test
    void rejectsCircularTableAndEmbeddedPropertyReferences() {
        assertMessage("Circular @TableRef reference detected", () -> build(CircularTableRefA.class));
        assertMessage("Circular property reference detected", () -> build(CircularEmbeddedRecord.class));
    }

    private TableDef build(Class<?> type) {
        return TableDefFactory.buildTableDef(GenericTypeFactory.build(type), configuration);
    }

    private PropertyDef property(TableDef tableDef, String name) {
        PropertyDef propertyDef = tableDef.getNameToPropertyDef().get(name);
        assertNotNull(propertyDef, name);
        return propertyDef;
    }

    private void assertMessage(String expected, Runnable action) {
        MybatisExtException exception = assertThrows(MybatisExtException.class, action::run);
        assertTrue(exception.getMessage().contains(expected), exception.getMessage());
    }

    @Table(name = "record", schema = "metadata")
    static class Record {
        @Id(idType = IdType.UUID)
        @Column(name = "record_id", jdbcType = JdbcType.BIGINT)
        private Long id;
        @Filterable(test = TestMode.NotEmpty, operator = CompareOperator.Like)
        @Column
        private String name;
        @Column
        private EmbeddedValue embedded;
        @Fetch(FetchType.LAZY)
        @Cascade
        @JoinRelation(table = TargetRecord.class, joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "id"), column = "name")
        private String targetName;
        private String computed;

        @Column
        public String getComputed() {
            return computed;
        }
    }

    static class EmbeddedValue {
        @Column(name = "code_value")
        private String code;
    }

    @Table
    static class TargetRecord {
        @Id
        @Column
        private Long id;
        @Column
        private String name;
    }

    @TableRef(Record.class)
    static class RecordView {
        @ColumnRef
        private Long id;
        @Filterable(operator = CompareOperator.Equals)
        @ColumnRef("name")
        private String displayName;
        @ColumnRef("embedded.code")
        private String code;
        @Fetch(FetchType.EAGER)
        @Cascade(false)
        @ColumnRef("targetName")
        private String eagerTargetName;
        private String readonlyName;

        @ColumnRef("name")
        public String getReadonlyName() {
            return readonlyName;
        }
    }

    @Table
    static class ParentRecord {
        @Column
        private Long parentId;
    }

    @Table
    @JoinParent(joinColumn = @JoinColumn(leftColumn = "parentId", rightColumn = "parentId"))
    static class ChildRecord extends ParentRecord {
        @Column
        private Long childId;
    }

    @Table
    static class RelationOwner {
        @Column
        private Long targetId;
        @JoinRelation(joinColumn = @JoinColumn(leftColumn = "targetId", rightColumn = "id"))
        private RelationTarget target;
        @Cascade
        @JoinRelation(joinColumn = @JoinColumn(leftColumn = "targetId", rightColumn = "id"))
        private RelationTarget cascadedTarget;
    }

    @Table
    static class RelationTarget {
        @Id
        @Column
        private Long id;
        @JoinRelation(table = TargetRecord.class, joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "id"), column = "name")
        private String relatedName;
    }

    static class PlainRecordSubclass extends Record {
    }

    static class UnmappedType {
    }

    @TableRef(Record.class)
    static class UnknownColumnRef {
        @ColumnRef("missing")
        private String value;
    }

    @TableRef(CircularTableRefB.class)
    static class CircularTableRefA {
    }

    @TableRef(CircularTableRefA.class)
    static class CircularTableRefB {
    }

    @Table
    static class CircularEmbeddedRecord {
        @Column
        private CircularEmbeddedValue value;
    }

    static class CircularEmbeddedValue {
        @Column
        private CircularEmbeddedValue child;
    }
}
