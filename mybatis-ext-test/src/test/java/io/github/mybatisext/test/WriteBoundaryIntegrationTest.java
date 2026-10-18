package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Filterable;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TestMode;
import io.github.mybatisext.mapper.BaseMapper;

class WriteBoundaryIntegrationTest {

    @Test
    void nullIdDoesNotBroadenUpdateOrDelete() throws Exception {
        try (SqlSession session = openSession(RowMapper.class)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            assertEquals(0, mapper.update(new Row(null, "changed")));
            assertEquals(0, mapper.delete(new Row(null, null)));
            assertOriginalRows(session);
        }
    }

    @Test
    void nullIdsInBatchDoNotAffectOtherRows() throws Exception {
        try (SqlSession session = openSession(RowMapper.class)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            mapper.updateBatch(Arrays.asList(new Row(null, "changed"), new Row(1L, "one")));
            assertOriginalRows(session);
            mapper.deleteBatch(Arrays.asList(new Row(null, null), new Row(null, null)));
            assertOriginalRows(session);
        }
    }

    @Test
    void incompleteCompositeIdDoesNotBroadenUpdateOrDelete() throws Exception {
        try (SqlSession session = openSession(CompositeMapper.class)) {
            CompositeMapper mapper = session.getMapper(CompositeMapper.class);
            for (CompositeRow row : new CompositeRow[]{new CompositeRow(1L, null), new CompositeRow(null, 10L), new CompositeRow(null, null)}) {
                assertEquals(0, mapper.update(row));
                assertEquals(0, mapper.delete(row));
                assertOriginalRows(session);
            }
        }
    }

    @Test
    void ignoreNullWithNoUpdateValuesFailsWithoutChangingRows() throws Exception {
        try (SqlSession session = openSession(RowMapper.class)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            assertSqlSyntaxError(() -> mapper.updateIgnoreNull(new Row(1L, null)));
            assertOriginalRows(session);
        }
    }

    @Test
    void fixedEmptyInFailsWithoutChangingRows() throws Exception {
        try (SqlSession session = openSession(RowMapper.class)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            assertSqlSyntaxError(() -> mapper.deleteByIdInIds(new Row(null, null), Collections.emptyList()));
            assertSqlSyntaxError(() -> mapper.updateNameByIdInIds(new Row(null, "changed"), Collections.emptyList()));
            assertOriginalRows(session);
        }
    }

    @Test
    void fixedEmptyNotInFailsWithoutChangingRows() throws Exception {
        try (SqlSession session = openSession(RowMapper.class)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            assertSqlSyntaxError(() -> mapper.updateNameByIdNotInIds(new Row(null, "changed"), Collections.emptyList()));
            assertSqlSyntaxError(() -> mapper.deleteByIdNotInIds(new Row(null, null), Collections.emptyList()));
            assertOriginalRows(session);
        }
    }

    @Test
    void nonEmptyInAndNotInRestrictWrites() throws Exception {
        try (SqlSession session = openSession(RowMapper.class)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            assertEquals(1, mapper.updateNameByIdInIds(new Row(null, "changed"), Collections.singletonList(1L)));
            assertEquals(1, mapper.deleteByIdNotInIds(new Row(null, null), Collections.singletonList(1L)));
            session.rollback();
            assertOriginalRows(session);
            assertEquals(1, mapper.updateNameByIdNotInIds(new Row(null, "changed"), Collections.singletonList(1L)));
            assertEquals(1, mapper.deleteByIdInIds(new Row(null, null), Collections.singletonList(1L)));
            session.rollback();
            assertOriginalRows(session);
        }
    }

    @Test
    void enabledDynamicFilterRestrictsWrites() throws Exception {
        try (SqlSession session = openSession(RowMapper.class)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            assertEquals(1, mapper.updateName(new Row(1L, "changed")));
            assertEquals(1, mapper.remove(new Row(2L, null)));
            try (Statement statement = session.getConnection().createStatement(); ResultSet rows = statement.executeQuery("SELECT id, name FROM boundary_row")) {
                assertTrue(rows.next());
                assertEquals(1L, rows.getLong("id"));
                assertEquals("changed", rows.getString("name"));
                assertFalse(rows.next());
            }
        }
    }

    @Test
    void disabledDynamicFiltersCurrentlyAllowUnrestrictedUpdate() throws Exception {
        try (SqlSession session = openSession(RowMapper.class)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            assertEquals(2, mapper.updateName(new Row(null, "changed")));
            session.rollback();
            assertOriginalRows(session);
        }
    }

    @Test
    void disabledDynamicFiltersCurrentlyAllowUnrestrictedDelete() throws Exception {
        try (SqlSession session = openSession(RowMapper.class)) {
            RowMapper mapper = session.getMapper(RowMapper.class);
            assertEquals(2, mapper.remove(new Row(null, null)));
            session.rollback();
            assertOriginalRows(session);
        }
    }

    private void assertSqlSyntaxError(Executable operation) {
        PersistenceException exception = assertThrows(PersistenceException.class, operation);
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException) {
                assertTrue(cause instanceof SQLSyntaxErrorException);
                assertEquals("42001", ((SQLException) cause).getSQLState());
                return;
            }
        }
        fail("Expected an SQL syntax error", exception);
    }

    private SqlSession openSession(Class<?> mapperType) throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:write_boundary_" + System.nanoTime());
        Configuration configuration = ConfigurationFactory.create(new Environment("test", new JdbcTransactionFactory(), dataSource), new ExtContext());
        configuration.addMapper(mapperType);
        SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession();
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE boundary_row (id BIGINT, tenant_id BIGINT, name VARCHAR(50), PRIMARY KEY (id, tenant_id))");
            statement.execute("INSERT INTO boundary_row VALUES (1, 10, 'one'), (2, 10, 'two')");
        }
        session.commit(true);
        return session;
    }

    private void assertOriginalRows(SqlSession session) throws Exception {
        try (Statement statement = session.getConnection().createStatement(); ResultSet rows = statement.executeQuery("SELECT id, name FROM boundary_row ORDER BY id")) {
            for (int id = 1; id <= 2; id++) {
                assertTrue(rows.next());
                assertEquals(id, rows.getLong("id"));
                assertEquals(id == 1 ? "one" : "two", rows.getString("name"));
            }
            assertFalse(rows.next());
        }
    }

    @Table(name = "boundary_row")
    public static class Row {

        @Id
        @Column
        public Long id;

        @Column
        @Filterable(test = TestMode.False)
        public String name;

        public Row(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Table(name = "boundary_row")
    public static class CompositeRow {

        @Id
        @Column
        public Long id;

        @Id
        @Column(name = "tenant_id")
        public Long tenantId;

        @Column
        public String name = "changed";

        public CompositeRow(Long id, Long tenantId) {
            this.id = id;
            this.tenantId = tenantId;
        }
    }

    public interface CompositeMapper extends BaseMapper<CompositeRow> {
    }

    public interface RowMapper extends BaseMapper<Row> {

        int deleteByIdInIds(@Param("row") Row row, @Param("ids") List<Long> ids);

        int deleteByIdNotInIds(@Param("row") Row row, @Param("ids") List<Long> ids);

        int updateNameByIdInIds(@Param("row") Row row, @Param("ids") List<Long> ids);

        int updateNameByIdNotInIds(@Param("row") Row row, @Param("ids") List<Long> ids);

        int remove(Row row);

        int updateName(Row row);
    }
}
