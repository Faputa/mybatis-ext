package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.mapper.BaseMapper;

class BatchTransactionIntegrationTest {

    private JdbcDataSource dataSource;
    private Connection keeper;
    private SqlSessionFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:batch_transaction_" + System.nanoTime());
        keeper = dataSource.getConnection();
        try (Statement statement = keeper.createStatement()) {
            statement.execute("CREATE TABLE transaction_row (id BIGINT PRIMARY KEY, name VARCHAR(50) NOT NULL)");
            statement.execute("INSERT INTO transaction_row VALUES (1, 'one')");
        }
        Configuration configuration = ConfigurationFactory.create(new Environment("test", new JdbcTransactionFactory(), dataSource), new ExtContext());
        configuration.addMapper(RowMapper.class);
        factory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @AfterEach
    void tearDown() throws Exception {
        keeper.close();
    }

    @Test
    void simpleExecutorRollsBackEarlierWriteAfterConstraintFailure() throws Exception {
        try (SqlSession session = factory.openSession(ExecutorType.SIMPLE)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            assertEquals(1, mapper.save(new Row(2L, "two")));
            assertConstraintViolation(() -> mapper.save(new Row(1L, "duplicate")));
            session.rollback();
        }
        assertNames("one");
    }

    @Test
    void batchExecutorRollsBackAfterFlushFailure() throws Exception {
        try (SqlSession session = factory.openSession(ExecutorType.BATCH)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            assertEquals(BatchExecutor.BATCH_UPDATE_RETURN_VALUE, mapper.save(new Row(2L, "two")));
            mapper.save(new Row(1L, "duplicate"));
            assertConstraintViolation(() -> session.flushStatements());
            session.rollback();
        }
        assertNames("one");
    }

    @Test
    void batchExecutorRollsBackAfterCommitFailure() throws Exception {
        try (SqlSession session = factory.openSession(ExecutorType.BATCH)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            mapper.save(new Row(2L, "two"));
            mapper.save(new Row(3L, null));
            assertConstraintViolation(() -> session.commit());
            session.rollback();
        }
        assertNames("one");
    }

    @Test
    void successfulFlushRemainsUncommittedUntilCommit() throws Exception {
        try (SqlSession session = factory.openSession(ExecutorType.BATCH)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            mapper.save(new Row(2L, "two"));
            mapper.save(new Row(3L, "three"));
            assertEquals(1, session.flushStatements().size());
            assertNames("one");
            session.commit();
        }
        assertNames("one", "two", "three");
    }

    @Test
    void successfulFlushCanStillBeRolledBack() throws Exception {
        try (SqlSession session = factory.openSession(ExecutorType.BATCH)) {
            session.getMapper(RowMapper.class).save(new Row(2L, "two"));
            session.flushStatements();
            session.rollback();
        }
        assertNames("one");
    }

    @Test
    void rollsBackMultiRowAndMultipleStatementBatchFailures() throws Exception {
        for (ExecutorType executorType : new ExecutorType[]{ExecutorType.SIMPLE, ExecutorType.BATCH}) {
            for (boolean ignoreNull : new boolean[]{false, true}) {
                try (SqlSession session = factory.openSession(executorType)) {
                    RowMapper mapper = session.getMapper(RowMapper.class);
                    assertConstraintViolation(() -> {
                        if (ignoreNull) {
                            mapper.saveBatchIgnoreNull(Arrays.asList(new Row(2L, "two"), new Row(1L, "duplicate")));
                        } else {
                            mapper.saveBatch(Arrays.asList(new Row(2L, "two"), new Row(1L, "duplicate")));
                        }
                        session.flushStatements();
                    });
                    session.rollback();
                }
                assertNames("one");
            }
        }
    }

    @Test
    void rollsBackBatchUpdateWhenALaterRowViolatesConstraint() throws Exception {
        try (Statement statement = keeper.createStatement()) {
            statement.execute("INSERT INTO transaction_row VALUES (2, 'two')");
        }
        for (ExecutorType executorType : new ExecutorType[]{ExecutorType.SIMPLE, ExecutorType.BATCH}) {
            try (SqlSession session = factory.openSession(executorType)) {
                assertConstraintViolation(() -> {
                    session.getMapper(RowMapper.class).updateBatch(Arrays.asList(new Row(1L, "changed"), new Row(2L, null)));
                    session.flushStatements();
                });
                session.rollback();
            }
            assertNames("one", "two");
        }
    }

    @Test
    void rollsBackBatchDeleteWhenALaterRowIsReferenced() throws Exception {
        try (Statement statement = keeper.createStatement()) {
            statement.execute("INSERT INTO transaction_row VALUES (2, 'two')");
            statement.execute("CREATE TABLE transaction_child (parent_id BIGINT REFERENCES transaction_row(id))");
            statement.execute("INSERT INTO transaction_child VALUES (1)");
        }
        for (ExecutorType executorType : new ExecutorType[]{ExecutorType.SIMPLE, ExecutorType.BATCH}) {
            try (SqlSession session = factory.openSession(executorType)) {
                assertConstraintViolation(() -> {
                    session.getMapper(RowMapper.class).deleteBatch(Arrays.asList(new Row(2L, null), new Row(1L, null)));
                    session.flushStatements();
                });
                session.rollback();
            }
            assertNames("one", "two");
        }
    }

    private void assertConstraintViolation(Executable operation) {
        PersistenceException exception = assertThrows(PersistenceException.class, operation);
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException) {
                String sqlState = ((SQLException) cause).getSQLState();
                if (sqlState != null && sqlState.startsWith("23")) {
                    return;
                }
            }
        }
        fail("Expected an integrity constraint violation", exception);
    }

    private void assertNames(String... expected) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT name FROM transaction_row ORDER BY id")) {
            for (String name : expected) {
                assertTrue(rows.next());
                assertEquals(name, rows.getString(1));
            }
            assertFalse(rows.next());
        }
    }

    @Table(name = "transaction_row")
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
