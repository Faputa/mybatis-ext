package io.github.mybatisext.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.mapper.BaseMapper;

class SpringTransactionIntegrationTest {

    private JdbcDataSource dataSource;
    private Connection keeper;
    private SqlSessionFactory factory;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:spring_transaction_" + System.nanoTime());
        keeper = dataSource.getConnection();
        try (Statement statement = keeper.createStatement()) {
            statement.execute("CREATE TABLE spring_transaction_row (id BIGINT PRIMARY KEY, name VARCHAR(50) NOT NULL)");
            statement.execute("INSERT INTO spring_transaction_row VALUES (1, 'one')");
        }
        ExtSqlSessionFactoryBean factoryBean = new ExtSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factory = factoryBean.getObject();
        factory.getConfiguration().addMapper(RowMapper.class);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @AfterEach
    void tearDown() throws Exception {
        keeper.close();
    }

    @Test
    void rollsBackSimpleExecutorOnConstraintFailure() throws Exception {
        SqlSessionTemplate template = new SqlSessionTemplate(factory, ExecutorType.SIMPLE);
        RowMapper mapper = template.getMapper(RowMapper.class);
        assertThrows(DataIntegrityViolationException.class, () -> transaction.execute(status -> {
            assertEquals(1, mapper.save(new Row(2L, "two")));
            mapper.save(new Row(1L, "duplicate"));
            return null;
        }));
        assertNames("one");
    }

    @Test
    void rollsBackBatchExecutorWhenCommitFlushFails() throws Exception {
        SqlSessionTemplate template = new SqlSessionTemplate(factory, ExecutorType.BATCH);
        RowMapper mapper = template.getMapper(RowMapper.class);
        assertThrows(DataIntegrityViolationException.class, () -> transaction.execute(status -> {
            mapper.save(new Row(2L, "two"));
            mapper.save(new Row(1L, "duplicate"));
            return null;
        }));
        assertNames("one");
    }

    @Test
    void rollsBackMultipleStatementBatchOnConstraintFailure() throws Exception {
        RowMapper mapper = new SqlSessionTemplate(factory).getMapper(RowMapper.class);
        assertThrows(DataIntegrityViolationException.class, () -> transaction.execute(status -> {
            mapper.saveBatchIgnoreNull(Arrays.asList(new Row(2L, "two"), new Row(1L, "duplicate")));
            return null;
        }));
        assertNames("one");
    }

    @Test
    void rollsBackFlushedBatchWhenTransactionIsMarkedRollbackOnly() throws Exception {
        SqlSessionTemplate template = new SqlSessionTemplate(factory, ExecutorType.BATCH);
        transaction.execute(status -> {
            template.getMapper(RowMapper.class).save(new Row(2L, "two"));
            template.flushStatements();
            status.setRollbackOnly();
            return null;
        });
        assertNames("one");
    }

    @Test
    void commitsSuccessfulBatchAtTransactionBoundary() throws Exception {
        RowMapper mapper = new SqlSessionTemplate(factory, ExecutorType.BATCH).getMapper(RowMapper.class);
        transaction.execute(status -> {
            mapper.save(new Row(2L, "two"));
            mapper.save(new Row(3L, "three"));
            return null;
        });
        assertNames("one", "two", "three");
    }

    private void assertNames(String... expected) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT name FROM spring_transaction_row ORDER BY id")) {
            for (String name : expected) {
                assertTrue(rows.next());
                assertEquals(name, rows.getString(1));
            }
            assertFalse(rows.next());
        }
    }

    @Table(name = "spring_transaction_row")
    public static class Row {

        @Id
        @Column
        public Long id;

        @Column
        public String name;

        public Row(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public interface RowMapper extends BaseMapper<Row> {
    }
}
