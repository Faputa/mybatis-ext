package io.github.mybatisext.dialect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.jpa.Limit;
import io.github.mybatisext.jpa.OrderByElement;
import io.github.mybatisext.jpa.OrderByType;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.testcase.Student;

class SqlServerDialectTest {

    private static TableInfo tableInfo;
    private static List<PropertyInfo> selectItems;
    private static List<OrderByElement> orderBy;

    @BeforeAll
    static void beforeAll() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtContext extContext = new ExtContext();
        Configuration configuration = ConfigurationFactory.create(environment, extContext);
        GenericType genericType = GenericTypeFactory.build(Student.class);
        tableInfo = TableInfoFactory.buildTableInfo(genericType, configuration, extContext);
        selectItems = new ArrayList<>(tableInfo.getNameToPropertyInfo().values());

        OrderByElement orderByElement = new OrderByElement();
        orderByElement.setPropertyInfo(tableInfo.get("id"));
        orderByElement.setType(OrderByType.ASC);
        orderBy = Collections.singletonList(orderByElement);
    }

    @Test
    void defaultSelectorSelectsSqlServerDialect() {
        Dialect dialect = new DefaultDialectSelector().select("jdbc:sqlserver://localhost:1433;databaseName=test");

        assertSame(DefaultDialectSelector.SQL_SERVER_DIALECT, dialect);
    }

    @Test
    void sqlServer2012UsesOffsetFetch() {
        Limit limit = limit(5, 10);

        String sql = new SqlServerDialect().select(tableInfo, selectItems, null, false, orderBy, null, null, limit);

        assertTrue(sql.contains(" OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY"));
    }

    @Test
    void sqlServerUsesTopWhenOffsetIsMissing() {
        String sql = new SqlServerDialect().select(tableInfo, selectItems, null, false, null, null, null, limit(null, 10));

        assertTrue(sql.startsWith("SELECT TOP (10)"));
    }

    @Test
    void sqlServer2012AddsOrderByWhenMissing() {
        String sql = new SqlServerDialect().select(tableInfo, selectItems, null, false, null, null, null, limit(0, 10));

        assertTrue(sql.contains(" ORDER BY (SELECT NULL) OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY"));
    }

    @Test
    void sqlServer2012SupportsDynamicLimit() {
        Limit limit = dynamicLimit();

        String sql = new SqlServerDialect().select(tableInfo, selectItems, null, false, orderBy, null, null, limit);

        assertTrue(sql.contains("OFFSET #{offset} ROWS FETCH NEXT #{rowCount} ROWS ONLY"));
    }

    @Test
    void sqlServer2008UsesTopWithoutOffset() {
        String sql = new SqlServer2008Dialect().select(tableInfo, selectItems, null, true, orderBy, null, null, limit(null, 10));

        assertTrue(sql.startsWith("SELECT DISTINCT TOP (10)"));
    }

    @Test
    void sqlServer2008UsesRowNumberWithOffset() {
        String sql = new SqlServer2008Dialect().select(tableInfo, selectItems, null, false, orderBy, null, null, limit(5, 10));

        assertTrue(sql.contains("ROW_NUMBER() OVER ( ORDER BY [id] ASC ) AS __row_number"));
        assertTrue(sql.contains("WHERE __row_number &gt; 5 AND __row_number &lt;= 15"));
        assertTrue(sql.endsWith("ORDER BY __row_number"));
    }

    @Test
    void sqlServer2008IncludesHiddenOrderItem() {
        List<PropertyInfo> nameOnly = Collections.singletonList(tableInfo.get("name"));

        String sql = new SqlServer2008Dialect().select(tableInfo, nameOnly, null, false, orderBy, null, null, limit(5, 10));

        assertTrue(sql.startsWith("SELECT [name] FROM"));
        assertTrue(sql.contains(tableInfo.get("id") + " AS [id]"));
    }

    @Test
    void sqlServer2008SupportsDynamicLimit() {
        Limit limit = dynamicLimit();

        String sql = new SqlServer2008Dialect().select(tableInfo, selectItems, null, false, orderBy, null, null, limit);

        assertTrue(sql.contains("WHERE __row_number &gt; #{offset}"));
        assertTrue(sql.contains("<bind name=\"__endRow\" value=\"offset + rowCount\"/> #{__endRow}"));
    }

    @Test
    void sqlServerUsesSharedSyntax() {
        SqlServerDialect dialect = new SqlServerDialect();
        Variable parameter = new Variable("student", GenericTypeFactory.build(Student.class));

        String update = dialect.update(tableInfo, selectItems, parameter, null, false);
        String delete = dialect.delete(tableInfo, parameter, null);

        assertTrue(update.startsWith("UPDATE " + tableInfo.getJoinTableInfo().getAlias() + " SET "));
        assertTrue(update.contains(" FROM " + tableInfo.getName() + " " + tableInfo.getJoinTableInfo().getAlias()));
        assertTrue(delete.startsWith("DELETE " + tableInfo.getJoinTableInfo().getAlias() + " FROM "));
        assertEquals("SELECT CASE WHEN EXISTS (SELECT 1) THEN 1 ELSE 0 END", dialect.buildExists("SELECT 1"));
        assertEquals("= 1", dialect.isTrue());
        assertEquals("= 0", dialect.isFalse());
        assertEquals("[a]]b]", dialect.quote("a]b"));
    }

    private static Limit limit(Integer offset, int rowCount) {
        Limit limit = new Limit();
        limit.setOffset(offset);
        limit.setRowCount(rowCount);
        return limit;
    }

    private static Limit dynamicLimit() {
        Limit limit = new Limit();
        limit.setOffsetVariable(new Variable("offset", GenericTypeFactory.build(Integer.class)));
        limit.setRowCountVariable(new Variable("rowCount", GenericTypeFactory.build(Integer.class)));
        return limit;
    }
}
