package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Statement;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.annotation.OnlyById;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.mapper.ExtMapper;

class ArrayBatchCrudIntegrationTest {

    @Test
    void insertsArraysWithAndWithoutParamAnnotation() throws Exception {
        try (SqlSession session = openSession()) {
            ArrayMapper mapper = session.getMapper(ArrayMapper.class);
            assertEquals(2, mapper.saveBatch(new Row[]{new Row(3L, "three"), new Row(4L, "four")}));
            assertEquals(2, mapper.insertBatch(new Row[]{new Row(5L, "five"), new Row(6L, "six")}));
            assertEquals("three", mapper.name(3L));
            assertEquals("four", mapper.name(4L));
            assertEquals("five", mapper.name(5L));
            assertEquals("six", mapper.name(6L));
        }
    }

    @Test
    void updatesArraysWithAndWithoutParamAnnotation() throws Exception {
        try (SqlSession session = openSession()) {
            ArrayMapper mapper = session.getMapper(ArrayMapper.class);
            assertTrue(mapper.updateBatch(new Row[]{new Row(1L, "first"), new Row(2L, "second")}) > 0);
            assertEquals("first", mapper.name(1L));
            assertEquals("second", mapper.name(2L));
            assertTrue(mapper.modifyBatch(new Row[]{new Row(1L, "changed"), new Row(2L, null)}) > 0);
            assertEquals("changed", mapper.name(1L));
            assertNull(mapper.name(2L));
        }
    }

    @Test
    void deletesArraysWithAndWithoutParamAnnotation() throws Exception {
        try (SqlSession session = openSession()) {
            ArrayMapper mapper = session.getMapper(ArrayMapper.class);
            assertTrue(mapper.deleteBatch(new Row[]{new Row(1L, null), new Row(2L, null)}) > 0);
            assertEquals(0, mapper.count());
            mapper.saveBatch(new Row[]{new Row(3L, "three"), new Row(4L, "four")});
            assertTrue(mapper.removeBatch(new Row[]{new Row(3L, null), new Row(4L, null)}) > 0);
            assertEquals(0, mapper.count());
        }
    }

    @Test
    void preservesPerRowIgnoreNullSemantics() throws Exception {
        try (SqlSession session = openSession()) {
            ArrayMapper mapper = session.getMapper(ArrayMapper.class);
            assertTrue(mapper.saveBatchIgnoreNull(new Row[]{new Row(3L, "three"), new Row(4L, null)}) > 0);
            assertEquals("three", mapper.name(3L));
            assertEquals("default-name", mapper.name(4L));
            assertTrue(mapper.updateBatchIgnoreNull(new Row[]{new Row(3L, null), new Row(4L, "changed")}) > 0);
            assertEquals("three", mapper.name(3L));
            assertEquals("changed", mapper.name(4L));
        }
    }

    private SqlSession openSession() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:array_batch_" + System.nanoTime());
        Configuration configuration = ConfigurationFactory.create(new Environment("test", new JdbcTransactionFactory(), dataSource), new ExtContext());
        configuration.addMapper(ArrayMapper.class);
        SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession();
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE array_row (id BIGINT PRIMARY KEY, name VARCHAR(50) DEFAULT 'default-name', marker VARCHAR(50))");
            statement.execute("INSERT INTO array_row (id, name) VALUES (1, 'one'), (2, 'two')");
        }
        return session;
    }

    @Table(name = "array_row")
    public static class Row {
        @Id(idType = IdType.NONE)
        @Column
        public Long id;
        @Column
        public String name;
        @Column
        public String marker = "marker";

        public Row(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public interface ArrayMapper extends ExtMapper<Row> {
        int saveBatch(Row[] rows);
        int insertBatch(@Param("rows") Row[] rows);
        int saveBatchIgnoreNull(Row[] rows);
        int updateBatch(@OnlyById Row[] rows);
        int modifyBatch(@Param("rows") @OnlyById Row[] rows);
        int updateBatchIgnoreNull(@Param("rows") @OnlyById Row[] rows);
        int deleteBatch(@OnlyById Row[] rows);
        int removeBatch(@Param("rows") @OnlyById Row[] rows);
        long count();

        @Select("SELECT name FROM array_row WHERE id = #{id}")
        String name(@Param("id") Long id);
    }
}
