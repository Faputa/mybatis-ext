package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.FetchType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Fetch;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.mapper.ExtMapper;

class NestedMappingIntegrationTest {

    @Test
    void separatesSameNamedNestedQueries() throws Exception {
        try (SqlSession session = openSession(QueryMapper.class)) {
            QueryEntity entity = session.getMapper(QueryMapper.class).list().get(0);
            assertEquals("A", entity.first.label);
            assertEquals("OTHER", entity.second.label);
        }
    }

    @Test
    void separatesSameNamedNestedResultMaps() throws Exception {
        try (SqlSession session = openSession(ResultMapper.class)) {
            ResultEntity entity = session.getMapper(ResultMapper.class).list().get(0);
            assertEquals(1L, entity.first.detail.refId);
            assertEquals(2L, entity.second.detail.refId);
        }
    }

    private SqlSession openSession(Class<?> mapperType) throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:nested_mapping_" + System.nanoTime());
        Configuration configuration = ConfigurationFactory.create(new Environment("test", new JdbcTransactionFactory(), dataSource), new ExtContext());
        configuration.addMapper(mapperType);
        SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession();
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE parent_entity (id BIGINT, ref_a BIGINT, ref_b BIGINT)");
            statement.execute("CREATE TABLE child_entity (id BIGINT, name VARCHAR(20))");
            statement.execute("CREATE TABLE other_child (id BIGINT, name VARCHAR(20))");
            statement.execute("INSERT INTO parent_entity VALUES (1, 1, 2)");
            statement.execute("INSERT INTO child_entity VALUES (1, 'A'), (2, 'B')");
            statement.execute("INSERT INTO other_child VALUES (2, 'OTHER')");
        }
        return session;
    }

    @Table(name = "parent_entity")
    public static class QueryEntity {

        @Column
        public Long id;
        @Column
        public First first;
        @Column
        public Second second;
    }

    public static class First {

        @Column(name = "ref_a")
        public Long refId;
        @Fetch(FetchType.EAGER)
        @JoinRelation(table = Child.class, column = "name", joinColumn = @JoinColumn(leftColumn = "refId", rightColumn = "id"))
        public String label;
    }

    public static class Second {

        @Column(name = "ref_b")
        public Long refId;
        @Fetch(FetchType.EAGER)
        @JoinRelation(table = OtherChild.class, column = "name", joinColumn = @JoinColumn(leftColumn = "refId", rightColumn = "id"))
        public String label;
    }

    @Table(name = "child_entity")
    public static class Child {

        @Column
        public Long id;
        @Column
        public String name;
    }

    @Table(name = "other_child")
    public static class OtherChild {

        @Column
        public Long id;
        @Column
        public String name;
    }

    @Table(name = "parent_entity")
    public static class ResultEntity {

        @Column
        public Long id;
        @Column
        public FirstContainer first;
        @Column
        public SecondContainer second;
    }

    public static class FirstContainer {

        @Column
        public FirstDetail detail;
    }

    public static class SecondContainer {

        @Column
        public SecondDetail detail;
    }

    public static class FirstDetail {

        @Column(name = "ref_a")
        public Long refId;
    }

    public static class SecondDetail {

        @Column(name = "ref_b")
        public Long refId;
    }

    public interface QueryMapper extends ExtMapper<QueryEntity> {

        List<QueryEntity> list();
    }

    public interface ResultMapper extends ExtMapper<ResultEntity> {

        List<ResultEntity> list();
    }
}
