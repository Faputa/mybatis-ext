package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.annotations.Param;
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
import io.github.mybatisext.annotation.Filterable;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.mapper.ExtMapper;
import io.github.mybatisext.dialect.SqlServerDialect;
import io.github.mybatisext.dialect.SqlServer2008Dialect;

class MapperQueryRegressionIntegrationTest {

    @Test
    void dispatchesNoArgumentAndNamedOverloads() throws Exception {
        try (SqlSession session = openSession(NoArgMapper.class)) {
            NoArgMapper mapper = session.getMapper(NoArgMapper.class);
            assertEquals(2, mapper.count());
            assertEquals(1, mapper.count("one"));
            assertEquals(0, mapper.count("missing"));
        }
    }

    @Test
    void dispatchesPrimitiveAndReferenceOverloads() throws Exception {
        try (SqlSession session = openSession(PrimitiveMapper.class)) {
            PrimitiveMapper mapper = session.getMapper(PrimitiveMapper.class);
            assertEquals(1, mapper.count(1L));
            assertEquals(0, mapper.count(99L));
            assertEquals(1, mapper.count("two"));
        }
    }

    @Test
    void mergesPrimitiveAndWrapperOverloads() throws Exception {
        try (SqlSession session = openSession(BoxingMapper.class)) {
            BoxingMapper mapper = session.getMapper(BoxingMapper.class);
            assertEquals(1, mapper.count(1L));
            assertEquals(1, mapper.count(Long.valueOf(2)));
            assertEquals(0, mapper.count(99L));
            assertEquals(0, mapper.count((Long) null));
            assertEquals(1, mapper.count("one"));
        }
    }

    @Test
    void mergesUnannotatedPrimitiveAndWrapperOverloads() throws Exception {
        try (SqlSession session = openSession(ScalarBoxingMapper.class)) {
            ScalarBoxingMapper mapper = session.getMapper(ScalarBoxingMapper.class);
            assertEquals(1, mapper.countById(1L));
            assertEquals(1, mapper.countById(Long.valueOf(2)));
            assertEquals(0, mapper.countById(99L));
        }
    }

    @Test
    void isolatesDefaultFilterableBetweenConfigurations() throws Exception {
        ExtContext disabled = new ExtContext();
        disabled.setDefaultFilterable(false);
        Entity filter = new Entity();
        filter.name = "one";
        try (SqlSession first = openSession(EntityMapper.class, disabled);
                SqlSession second = openSession(EntityMapper.class)) {
            assertEquals(2, first.getMapper(EntityMapper.class).count(filter));
            assertEquals(1, second.getMapper(EntityMapper.class).count(filter));
            assertEquals(2, first.getMapper(EntityMapper.class).count(filter));
        }
    }

    @Test
    void joinsTablesReferencedOnlyByHaving() throws Exception {
        try (SqlSession session = openSession(HavingMapper.class)) {
            HavingMapper mapper = session.getMapper(HavingMapper.class);
            assertEquals(Collections.singletonList("one"), mapper.listNameGroupByNameOrderByName("child-one"));
            assertEquals(Collections.singletonList("two"), mapper.listNameGroupByNameOrderByName("child-two"));
            assertEquals(Collections.emptyList(), mapper.listNameGroupByNameOrderByName("missing"));
            assertEquals(Arrays.asList("one", "two"), mapper.listNameGroupByNameOrderByName(null));
        }
    }

    private SqlSession openSession(Class<?> mapperType) throws Exception {
        return openSession(mapperType, new ExtContext());
    }

    @Test
    void joinsHavingTablesWithSqlServerDialects() throws Exception {
        for (SqlServerDialect dialect : Arrays.asList(new SqlServerDialect(), new SqlServer2008Dialect())) {
            ExtContext context = new ExtContext();
            context.setDialectSelector(url -> dialect);
            try (SqlSession session = openSession(HavingMapper.class, context)) {
                try (Statement statement = session.getConnection().createStatement()) {
                    statement.execute("SET MODE MSSQLServer");
                }
                HavingMapper mapper = session.getMapper(HavingMapper.class);
                assertEquals(Collections.singletonList("one"), mapper.listNameGroupByNameOrderByName("child-one"));
                assertEquals(Arrays.asList("one", "two"), mapper.listNameGroupByNameOrderByName(null));
            }
        }
    }

    private SqlSession openSession(Class<?> mapperType, ExtContext context) throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:mapper_query_" + System.nanoTime());
        Configuration configuration = ConfigurationFactory.create(new Environment("test", new JdbcTransactionFactory(), dataSource), context);
        configuration.addMapper(mapperType);
        SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession();
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE query_entity (id BIGINT PRIMARY KEY, name VARCHAR(50))");
            statement.execute("CREATE TABLE query_child (entity_id BIGINT, name VARCHAR(50))");
            statement.execute("INSERT INTO query_entity VALUES (1, 'one'), (2, 'two')");
            statement.execute("INSERT INTO query_child VALUES (1, 'child-one'), (2, 'child-two')");
        }
        return session;
    }

    @Table(name = "query_entity", alias = "e")
    public static class Entity {
        @Column
        public Long id;
        @Column
        public String name;
        @JoinRelation(table = Child.class, tableAlias = "c", column = "name", joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "entityId"))
        public String childName;
    }

    @Table(name = "query_child")
    public static class Child {
        @Column
        public Long entityId;
        @Column
        public String name;
    }

    public interface NoArgMapper extends ExtMapper<Entity> {
        long count();
        long count(@Param("name") String name);
    }

    public interface PrimitiveMapper extends ExtMapper<Entity> {
        long count(@Param("id") long id);
        long count(@Param("name") String name);
    }

    public interface EntityMapper extends ExtMapper<Entity> {
        long count(Entity filter);
    }

    public interface BoxingMapper extends ExtMapper<Entity> {
        long count(@Param("id") long id);
        long count(@Param("id") Long id);
        long count(@Param("name") String name);
    }

    public interface ScalarBoxingMapper extends ExtMapper<Entity> {
        long countById(long id);
        long countById(Long id);
    }

    public interface HavingMapper extends ExtMapper<Entity> {
        List<String> listNameGroupByNameOrderByName(@Param("childName") @Filterable(exprTemplate = "MAX({propertyInfo}) = {variable.##placeholder}") String childName);
    }
}
