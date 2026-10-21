package io.github.mybatisext.statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.dialect.H2Dialect;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericParameter;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

class MappedStatementHelperTest {

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

    interface Mapper {
        long count(@Param("id") long id);
        long count(@Param("id") Long id);
    }

    @Table(name = "cache_entity")
    static class Entity {
        @Column
        private Long id;
    }
}
