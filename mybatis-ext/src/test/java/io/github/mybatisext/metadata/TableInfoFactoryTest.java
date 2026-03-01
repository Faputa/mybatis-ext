package io.github.mybatisext.metadata;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.testcase.StudentCourse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        TableInfoFactoryTest.configuration = ConfigurationFactory.create(environment, extContext);
    }

    @Test
    void testTablePermission() {
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
    }

    @Test
    void testTablePermissionEmbed() {
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
    void testTablePermissionVO() {
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
    void testStudentCourse() {
        GenericType genericType = GenericTypeFactory.build(StudentCourse.class);
        TableInfo tableInfo = TableInfoFactory.buildTableInfo(genericType, configuration, extContext);
        System.out.println(tableInfo.getAliasToJoinTableInfo().size());
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
}
