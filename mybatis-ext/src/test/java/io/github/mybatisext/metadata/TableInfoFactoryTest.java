package io.github.mybatisext.metadata;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.mapping.FetchType;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.ColumnRef;
import io.github.mybatisext.annotation.Fetch;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinParent;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.fixture.permission.RowPermission;
import io.github.mybatisext.fixture.permission.TablePermission;
import io.github.mybatisext.fixture.permission.TablePermissionEmbed;
import io.github.mybatisext.fixture.permission.TablePermissionVO;
import io.github.mybatisext.fixture.school.StudentCourse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.idgenerator.IdGenerator;

class TableInfoFactoryTest {

    private static ExtContext extContext;
    private static Configuration configuration;

    @BeforeAll
    static void beforeAll() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        TableInfoFactoryTest.extContext = new ExtContext();
        TableInfoFactoryTest.configuration = new Configuration(environment);
    }

    @Test
    void buildsMetadataForInheritedRelations() {
        GenericType genericType = GenericTypeFactory.build(TablePermission.class);
        TableInfo tableInfo = TableInfoFactory.buildTableInfo(genericType, configuration, extContext);
        assertEquals(5, tableInfo.getAliasToJoinTableInfo().size());
        assertTrue(tableInfo.get("tableId2").isReadonly());
        // 测试关联属性
        assertNotNull(tableInfo.get("dataSourceId"));
        assertNotNull(tableInfo.get("dataSourceName"));
        assertNotNull(tableInfo.get("dataSourceName2"));
        assertNotNull(tableInfo.get("dataTableName"));
        assertNotNull(tableInfo.get("schemaName"));
        assertNotNull(tableInfo.get("rowPermissions"));
        assertNotNull(tableInfo.get("rowPermissions2"));
        assertNotNull(tableInfo.get("columnPermissions"));
        // 测试父类关联属性
        assertNotNull(tableInfo.get("dataSourceName3"));
        assertEquals("table_permission", tableInfo.getTableName().getName());
        assertEquals("", tableInfo.getTableName().getSchema());
        assertEquals("tp", tableInfo.getJoinTableInfo().getAlias());
        assertEquals("role_id", tableInfo.get("roleId").getColumnName());
        assertNull(tableInfo.get("roleId").getJdbcType());
        assertEquals(PropertyType.COLLECTION, tableInfo.get("rowPermissions").getPropertyType());
        assertEquals(RowPermission.class, tableInfo.get("rowPermissions").getOfType().getType());
        assertNull(tableInfo.get("rowPermissions").getFetchType());
        assertEquals(FetchType.DEFAULT, tableInfo.get("rowPermissions2").getFetchType());
        assertEquals("schema_name", tableInfo.get("schemaName").getColumnName());
    }

    @Test
    void buildsMetadataForEmbeddedRelations() {
        GenericType genericType = GenericTypeFactory.build(TablePermissionEmbed.class);
        TableInfo tableInfo = TableInfoFactory.buildTableInfo(genericType, configuration, extContext);
        assertEquals(5, tableInfo.getAliasToJoinTableInfo().size());
        assertTrue(tableInfo.get("tablePermission").get("tableId2").isReadonly());
        // 测试子属性关联属性
        assertNotNull(tableInfo.get("tablePermission").get("dataSourceId"));
        assertNotNull(tableInfo.get("tablePermission").get("dataSourceName"));
        assertNotNull(tableInfo.get("tablePermission").get("dataSourceName2"));
        assertNotNull(tableInfo.get("tablePermission").get("dataTableName"));
        assertNotNull(tableInfo.get("tablePermission").get("schemaName"));
        assertNotNull(tableInfo.get("tablePermission").get("rowPermissions"));
        assertNotNull(tableInfo.get("tablePermission").get("rowPermissions2"));
        assertNotNull(tableInfo.get("tablePermission").get("columnPermissions"));
        // 测试子属性父类关联属性
        assertNotNull(tableInfo.get("tablePermission").get("dataSourceName3"));
        // 测试使用子属性关联
        assertNotNull(tableInfo.get("schemaName"));
    }

    @Test
    void buildsMetadataForReferencedDto() {
        GenericType genericType = GenericTypeFactory.build(TablePermissionVO.class);
        TableInfo tableInfo = TableInfoFactory.buildTableInfo(genericType, configuration, extContext);
        assertEquals(5, tableInfo.getAliasToJoinTableInfo().size());
        assertTrue(tableInfo.get("tableId2").isReadonly());
        // 测试关联属性
        assertNotNull(tableInfo.get("dataSourceId"));
        assertNotNull(tableInfo.get("dataSourceName"));
        assertNotNull(tableInfo.get("dataSourceName2"));
        assertNotNull(tableInfo.get("dataTableName"));
        assertNotNull(tableInfo.get("schemaName"));
        assertNotNull(tableInfo.get("rowPermissions"));
        assertNotNull(tableInfo.get("rowPermissions2"));
        assertNotNull(tableInfo.get("columnPermissions"));
        // 测试引用父类属性
        assertNotNull(tableInfo.get("description"));
        // 测试引用父类关联属性
        assertNotNull(tableInfo.get("dataSourceName3"));
        // 测试未启用级联
        assertNull(tableInfo.get("dataTable").get("dataSourceName3"));
        // 测试启用级联
        assertNotNull(tableInfo.get("dataTable2").get("dataSourceName3"));
    }

    @Test
    void buildsMetadataForCascadedStudentCourseGraph() {
        GenericType genericType = GenericTypeFactory.build(StudentCourse.class);
        TableInfo tableInfo = TableInfoFactory.buildTableInfo(genericType, configuration, extContext);
        assertEquals(20, tableInfo.getAliasToJoinTableInfo().size());
        // 测试属性
        assertNotNull(tableInfo.get("studentId"));
        assertNotNull(tableInfo.get("courseId"));
        assertNotNull(tableInfo.get("selectDate"));
        assertNotNull(tableInfo.get("student"));
        assertNotNull(tableInfo.get("course"));
        assertNotNull(tableInfo.get("studentExams"));
        assertNotNull(tableInfo.get("studentHomeworks"));
        // 测试关联属性
        assertNotNull(tableInfo.get("student").get("id"));
        assertNotNull(tableInfo.get("student").get("classId"));
        assertNotNull(tableInfo.get("student").get("name"));
        assertNotNull(tableInfo.get("student").get("gender"));
        assertNotNull(tableInfo.get("student").get("age"));
        // 测试未启用级联
        assertNull(tableInfo.get("student").get("studentCourses"));
        assertNull(tableInfo.get("student").get("studentExams"));
        assertNull(tableInfo.get("student").get("studentHomeworks"));
        // 测试启用级联
        assertNotNull(tableInfo.get("course").get("students"));
        assertNotNull(tableInfo.get("course").get("studentExams"));
        assertNotNull(tableInfo.get("course").get("studentHomeworks"));
    }

    @Test
    void columnRefSupportsRenamingAndBothDtoStyles() {
        TableInfo inherited = buildTableInfo(InheritedDto.class);
        TableInfo selected = buildTableInfo(SelectedDto.class);

        assertTrue(inherited.getNameToPropertyInfo().keySet().containsAll(java.util.Arrays.asList("id", "source", "createdBy")));
        assertEquals(2, selected.getNameToPropertyInfo().size());
        assertNotNull(selected.get("id"));
        assertEquals("source_value", selected.get("displayName").getColumnName());
        assertEquals(JdbcType.VARCHAR, selected.get("displayName").getJdbcType());
    }

    @Test
    void tableAndColumnDefaultsAndOverridesAreMaterialized() {
        TableInfo tableInfo = buildTableInfo(AnnotatedEntity.class);

        assertEquals("training.annotated_entity", tableInfo.getName());
        assertEquals("ae", tableInfo.getJoinTableInfo().getAlias());
        assertEquals("display_name", tableInfo.get("name").getColumnName());
        assertEquals(JdbcType.VARCHAR, tableInfo.get("name").getJdbcType());
        assertEquals("getter_value", tableInfo.get("getterValue").getColumnName());

        TableInfo fetchOwner = buildTableInfo(FetchOwner.class);
        assertEquals(FetchType.LAZY, fetchOwner.get("target").getFetchType());
    }

    @Test
    void rejectsCircularEmbeddedProperties() {
        MybatisExtException exception = assertThrows(MybatisExtException.class, () -> buildTableInfo(CircularEmbeddedEntity.class));

        assertTrue(exception.getMessage().contains("Circular property reference detected"));
        assertTrue(exception.getMessage().contains("CircularEmbeddedValue.child"));
    }

    @Test
    void rejectsCircularTableReferences() {
        MybatisExtException exception = assertThrows(MybatisExtException.class, () -> buildTableInfo(CircularTableRefA.class));

        assertTrue(exception.getMessage().contains("Circular @TableRef reference detected"));
    }

    @Test
    void rejectsUnknownLeftTableAlias() {
        MybatisExtException exception = assertThrows(MybatisExtException.class, () -> buildTableInfo(UnknownAliasOwner.class));

        assertTrue(exception.getMessage().contains("Unknown left table alias 'missing'"));
    }

    @Test
    void rejectsDuplicateAliasesWithinRelationPath() {
        MybatisExtException exception = assertThrows(MybatisExtException.class, () -> buildTableInfo(DuplicateAliasOwner.class));

        assertTrue(exception.getMessage().contains("Duplicate table alias 'target'"));
    }

    @Test
    void rejectsJoinParentWithoutSuperclass() {
        MybatisExtException exception = assertThrows(MybatisExtException.class, () -> buildTableInfo(InvalidJoinParent.class));

        assertTrue(exception.getMessage().contains("requires a superclass"));
    }

    @Test
    void rejectsCustomIdGeneratorsThatCannotBeInstantiated() {
        MybatisExtException exception = assertThrows(MybatisExtException.class, () -> buildTableInfo(InvalidIdGeneratorEntity.class));

        assertTrue(exception.getMessage().contains("customIdGenerator cannot be instantiated"));
    }

    @Test
    void resolvesPropertyPathsAndUnderlyingTableTypes() {
        TableInfo tableInfo = buildTableInfo(TablePermissionEmbed.class);

        assertEquals("schema_name", TableInfoFactory.getPropertyInfo(tableInfo, "tablePermission.schemaName").getColumnName());
        MybatisExtException exception = assertThrows(MybatisExtException.class, () -> TableInfoFactory.getPropertyInfo(tableInfo, "tablePermission.missing"));
        assertTrue(exception.getMessage().contains("Property 'tablePermission.missing' not found"));
        assertTrue(TableInfoFactory.isSameTableType(GenericTypeFactory.build(TrainingEntity.class), GenericTypeFactory.build(SelectedDto.class)));
        assertFalse(TableInfoFactory.isSameTableType(GenericTypeFactory.build(TrainingEntity.class), GenericTypeFactory.build(Object.class)));
    }

    private TableInfo buildTableInfo(Class<?> type) {
        return TableInfoFactory.buildTableInfo(GenericTypeFactory.build(type), configuration, extContext);
    }

    @Table(name = "training_entity")
    static class TrainingEntity {
        @Column
        private Long id;
        @Column(name = "source_value", jdbcType = JdbcType.VARCHAR)
        private String source;
        @Column
        private String createdBy;
    }

    @TableRef(TrainingEntity.class)
    static class InheritedDto extends TrainingEntity {
    }

    @TableRef(TrainingEntity.class)
    static class SelectedDto {
        @ColumnRef
        private Long id;
        @ColumnRef("source")
        private String displayName;
    }

    @Table(name = "annotated_entity", schema = "training", alias = "ae")
    static class AnnotatedEntity {
        @Column(name = "display_name", jdbcType = JdbcType.VARCHAR)
        private String name;
        private String getterValue;

        @Column
        public String getGetterValue() {
            return getterValue;
        }
    }

    @Table
    static class FetchTarget {
        @Id
        @Column
        private Long id;
    }

    @Table
    static class FetchOwner {
        @Column
        private Long targetId;
        @Fetch(FetchType.LAZY)
        @JoinRelation(joinColumn = @JoinColumn(leftColumn = "targetId", rightColumn = "id"))
        private FetchTarget target;
    }

    @Table
    static class CircularEmbeddedEntity {
        @Column
        private CircularEmbeddedValue value;
    }

    static class CircularEmbeddedValue {
        @Column
        private CircularEmbeddedValue child;
    }

    @TableRef(CircularTableRefB.class)
    static class CircularTableRefA {
    }

    @TableRef(CircularTableRefA.class)
    static class CircularTableRefB {
    }

    @Table
    static class AliasTarget {
        @Id
        @Column
        private Long id;
    }

    @Table
    static class UnknownAliasOwner {
        @Column
        private Long targetId;
        @JoinRelation(table = AliasTarget.class, joinColumn = @JoinColumn(leftTableAlias = "missing", leftColumn = "targetId", rightColumn = "id"))
        private String targetName;
    }

    @Table
    static class DuplicateAliasOwner {
        @Column
        private Long targetId;
        @JoinRelation(table = AliasTarget.class, tableAlias = "target", joinColumn = @JoinColumn(leftColumn = "targetId", rightColumn = "id"))
        @JoinRelation(table = AliasTarget.class, tableAlias = "target", joinColumn = @JoinColumn(leftTableAlias = "target", leftColumn = "id", rightColumn = "id"))
        private String targetName;
    }

    @Table
    @JoinParent(joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "id"))
    static class InvalidJoinParent {
        @Column
        private Long id;
    }

    @Table
    static class InvalidIdGeneratorEntity {
        @Id(idType = IdType.CUSTOM, customIdGenerator = InaccessibleIdGenerator.class)
        @Column
        private String id;
    }

    static class InaccessibleIdGenerator implements IdGenerator<String> {
        private InaccessibleIdGenerator() {
        }

        @Override
        public String getId() {
            return "id";
        }
    }
}
