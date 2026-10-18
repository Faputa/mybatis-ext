package io.github.mybatisext.statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Filterable;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TestMode;
import io.github.mybatisext.dialect.H2Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.CompareOperator;
import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericParameter;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

class MappedStatementHelperTest {

    @Test
    void rendersEquivalentOverloadsOnceForNullAndNonNullArguments() throws Exception {
        try (BasicDataSource dataSource = new BasicDataSource()) {
            dataSource.setUrl("jdbc:h2:mem:semantic_overlapping");
            try (Connection connection = dataSource.getConnection(); Statement schema = connection.createStatement()) {
                schema.execute("CREATE TABLE cache_entity (id BIGINT PRIMARY KEY, name VARCHAR(50))");
                schema.execute("INSERT INTO cache_entity VALUES (1, 'one'), (2, 'two')");
                Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
                ExtContext context = new ExtContext();
                AtomicInteger scripts = new AtomicInteger();
                context.setDialectSelector(url -> new H2Dialect() {

                    @Override
                    public String count(TableInfo tableInfo, Condition where) {
                        scripts.incrementAndGet();
                        return super.count(tableInfo, where);
                    }
                });
                GenericMethod first = new GenericMethod(OverlappingMapper.class.getMethod("count", long.class, String.class), Collections.emptyMap());
                GenericMethod second = new GenericMethod(OverlappingMapper.class.getMethod("count", long.class, Integer.class), Collections.emptyMap());
                GenericMethod different = new GenericMethod(OverlappingMapper.class.getMethod("count", String.class), Collections.emptyMap());
                for (List<GenericMethod> methods : Arrays.asList(Arrays.asList(first, second), Arrays.asList(second, first),
                        Arrays.asList(first, second, different), Arrays.asList(different, second, first))) {
                    scripts.set(0);
                    MappedStatementHelper helper = new MappedStatementHelper(configuration, context, new TableInfoFactory(configuration, context));
                    MappedStatement statement = helper.build(OverlappingMapper.class.getName() + ".count", GenericTypeFactory.build(Entity.class), methods, GenericTypeFactory.build(long.class), false);
                    for (Object ignoredName : new Object[]{null, "ignored", 7}) {
                        MapperMethod.ParamMap<Object> parameters = new MapperMethod.ParamMap<>();
                        parameters.put("id", 1L);
                        parameters.put("param1", 1L);
                        parameters.put("name", ignoredName);
                        parameters.put("param2", ignoredName);
                        BoundSql bound = statement.getBoundSql(parameters);
                        assertEquals(1, bound.getParameterMappings().size(), bound.getSql());
                        assertEquals("id", bound.getParameterMappings().get(0).getProperty());
                        try (PreparedStatement query = connection.prepareStatement(bound.getSql())) {
                            query.setLong(1, 1L);
                            try (ResultSet rows = query.executeQuery()) {
                                assertTrue(rows.next());
                                assertEquals(1L, rows.getLong(1));
                                assertFalse(rows.next());
                            }
                        }
                    }
                    assertEquals(methods.size() == 2 ? 1 : 2, scripts.get());
                }
            }
        }
    }

    @Test
    void rejectsNormalizedSignatureConflictsRegardlessOfMethodOrder() throws Exception {
        try (BasicDataSource dataSource = new BasicDataSource()) {
            dataSource.setUrl("jdbc:h2:mem:semantic_conflict");
            Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
            ExtContext context = new ExtContext();
            GenericMethod primitive = new GenericMethod(ConflictingMapper.class.getMethod("count", long.class), Collections.emptyMap());
            GenericMethod wrapper = new GenericMethod(ConflictingMapper.class.getMethod("count", Long.class), Collections.emptyMap());
            GenericMethod equivalent = new GenericMethod(ConflictingMapper.class.getMethod("count", long.class, String.class), Collections.emptyMap());
            String id = ConflictingMapper.class.getName() + ".count";
            for (List<GenericMethod> methods : Arrays.asList(
                    Arrays.asList(primitive, wrapper), Arrays.asList(wrapper, primitive),
                    Arrays.asList(primitive, wrapper, equivalent), Arrays.asList(primitive, equivalent, wrapper),
                    Arrays.asList(wrapper, primitive, equivalent), Arrays.asList(wrapper, equivalent, primitive),
                    Arrays.asList(equivalent, primitive, wrapper), Arrays.asList(equivalent, wrapper, primitive))) {
                MappedStatementHelper helper = new MappedStatementHelper(configuration, context, new TableInfoFactory(configuration, context));
                for (boolean writeConfiguration : new boolean[]{false, true}) {
                    MybatisExtException exception = assertThrows(MybatisExtException.class,
                            () -> helper.build(id, GenericTypeFactory.build(Entity.class), methods, GenericTypeFactory.build(long.class), writeConfiguration));
                    assertTrue(exception.getMessage().contains(id));
                    assertTrue(exception.getMessage().contains("java.lang.Long"));
                }
            }
        }
    }

    @Test
    void rejectsConflictingOverloadsBeforeOpeningDatabaseConnection() throws Exception {
        AtomicInteger connections = new AtomicInteger();
        try (BasicDataSource dataSource = new BasicDataSource() {

            @Override
            public Connection getConnection() throws SQLException {
                connections.incrementAndGet();
                throw new SQLException("Database unavailable");
            }
        }) {
            Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
            ExtContext context = new ExtContext();
            MappedStatementHelper helper = new MappedStatementHelper(configuration, context, new TableInfoFactory(configuration, context));
            List<GenericMethod> methods = Arrays.asList(
                    new GenericMethod(ConflictingMapper.class.getMethod("count", long.class), Collections.emptyMap()),
                    new GenericMethod(ConflictingMapper.class.getMethod("count", Long.class), Collections.emptyMap()));
            String id = ConflictingMapper.class.getName() + ".count";
            MybatisExtException exception = assertThrows(MybatisExtException.class,
                    () -> helper.build(id, GenericTypeFactory.build(Entity.class), methods, GenericTypeFactory.build(long.class), false));
            assertTrue(exception.getMessage().contains("Conflicting overload semantics"));
            assertTrue(exception.getMessage().contains(id));
            assertEquals(0, connections.get());
        }
    }

    @Test
    void preservesAllSignaturesWhenEquivalentAndDifferentSemanticsAreMixed() throws Exception {
        try (BasicDataSource dataSource = new BasicDataSource()) {
            dataSource.setUrl("jdbc:h2:mem:semantic_signatures");
            Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
            ExtContext context = new ExtContext();
            GenericMethod first = new GenericMethod(MixedMapper.class.getMethod("count", long.class), Collections.emptyMap());
            GenericMethod equivalent = new GenericMethod(MixedMapper.class.getMethod("count", long.class, String.class), Collections.emptyMap());
            GenericMethod different = new GenericMethod(MixedMapper.class.getMethod("count", String.class), Collections.emptyMap());
            for (List<GenericMethod> methods : Arrays.asList(Arrays.asList(first, equivalent, different), Arrays.asList(different, equivalent, first))) {
                MappedStatementHelper helper = new MappedStatementHelper(configuration, context, new TableInfoFactory(configuration, context));
                MappedStatement statement = helper.build(MixedMapper.class.getName() + ".count", GenericTypeFactory.build(Entity.class), methods, GenericTypeFactory.build(long.class), false);
                Map<String, Object> byId = new MapperMethod.ParamMap<>();
                byId.put("id", 1L);
                byId.put("param1", 1L);
                Map<String, Object> byIdWithIgnoredName = new MapperMethod.ParamMap<>();
                byIdWithIgnoredName.putAll(byId);
                byIdWithIgnoredName.put("name", "ignored");
                byIdWithIgnoredName.put("param2", "ignored");
                Map<String, Object> byName = new MapperMethod.ParamMap<>();
                byName.put("name", "one");
                byName.put("param1", "one");
                String sql = statement.getBoundSql(byId).getSql();
                assertFalse(sql.isEmpty());
                assertEquals(1, statement.getBoundSql(byId).getParameterMappings().size());
                assertEquals(sql, statement.getBoundSql(byIdWithIgnoredName).getSql());
                assertEquals("id", statement.getBoundSql(byIdWithIgnoredName).getParameterMappings().get(0).getProperty());
                assertEquals("name", statement.getBoundSql(byName).getParameterMappings().get(0).getProperty());
            }
        }
    }

    @Test
    void reusesParsedOverloadsBetweenValidationAndExecutionWithinOneHelper() throws Exception {
        try (BasicDataSource dataSource = new BasicDataSource()) {
            dataSource.setUrl("jdbc:h2:mem:semantic_cache");
            Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
            ExtContext context = new ExtContext();
            AtomicInteger parses = new AtomicInteger();
            List<GenericMethod> methods = Arrays.asList(countingMethod(long.class, parses), countingMethod(Long.class, parses));
            GenericType tableType = GenericTypeFactory.build(Entity.class);
            GenericType returnType = GenericTypeFactory.build(long.class);
            String id = Mapper.class.getName() + ".count";
            MappedStatementHelper helper = new MappedStatementHelper(configuration, context, new TableInfoFactory(configuration, context));

            MappedStatement validation = helper.build(id, tableType, methods, returnType, false);
            MappedStatement execution = helper.build(id, tableType, methods, returnType, true);
            helper.build(id, tableType, methods, returnType, false);
            assertEquals(2, parses.get());
            assertEquals(validation.getBoundSql(Collections.singletonMap("id", 1L)).getSql(), execution.getBoundSql(Collections.singletonMap("id", 1L)).getSql());

            Configuration otherConfiguration = new Configuration(configuration.getEnvironment());
            MappedStatementHelper other = new MappedStatementHelper(otherConfiguration, context, new TableInfoFactory(otherConfiguration, context));
            other.build(id, tableType, methods, returnType, false);
            assertEquals(4, parses.get());
        }
    }

    @Test
    void mergesEquivalentOverloadsWithoutComparingDialectOutput() throws Exception {
        try (BasicDataSource dataSource = new BasicDataSource()) {
            dataSource.setUrl("jdbc:h2:mem:semantic_dialect");
            Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
            ExtContext context = new ExtContext();
            AtomicInteger scripts = new AtomicInteger();
            context.setDialectSelector(url -> new H2Dialect() {

                @Override
                public String count(TableInfo tableInfo, Condition where) {
                    return "/* generation " + scripts.incrementAndGet() + " */ " + super.count(tableInfo, where);
                }
            });
            List<GenericMethod> methods = Arrays.asList(countingMethod(long.class, new AtomicInteger()), countingMethod(Long.class, new AtomicInteger()));
            MappedStatementHelper helper = new MappedStatementHelper(configuration, context, new TableInfoFactory(configuration, context));
            helper.build(Mapper.class.getName() + ".count", GenericTypeFactory.build(Entity.class), methods, GenericTypeFactory.build(long.class), false);
            assertEquals(1, scripts.get());
        }
    }

    private GenericMethod countingMethod(Class<?> parameterType, AtomicInteger parses) throws Exception {
        return new GenericMethod(Mapper.class.getMethod("count", parameterType), Collections.emptyMap()) {

            @Override
            public GenericParameter[] getParameters() {
                parses.incrementAndGet();
                return super.getParameters();
            }
        };
    }

    interface OverlappingMapper {

        long count(@Param("id") long id, @Param("name") @Filterable(test = TestMode.False) String name);

        long count(@Param("id") long id, @Param("name") @Filterable(test = TestMode.False) Integer name);

        long count(@Param("name") String name);
    }

    interface ConflictingMapper {

        long count(@Param("id") long id);

        long count(@Param("id") @Filterable(operator = CompareOperator.GreaterThan) Long id);

        long count(@Param("id") long id, @Param("name") @Filterable(test = TestMode.False) String name);
    }

    interface MixedMapper {

        long count(@Param("id") long id);

        long count(@Param("id") long id, @Param("name") @Filterable(test = TestMode.False) String name);

        long count(@Param("name") String name);
    }

    interface Mapper {

        long count(@Param("id") long id);

        long count(@Param("id") Long id);
    }

    @Table(name = "cache_entity")
    static class Entity {

        @Column
        private Long id;

        @Column
        private String name;
    }
}
