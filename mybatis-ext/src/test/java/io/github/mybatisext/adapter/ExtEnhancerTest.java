package io.github.mybatisext.adapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.mapper.ExtMapper;

class ExtEnhancerTest {

    @Test
    void returnsExistingStatementsAndRejectsUnknownNamespaces() {
        Configuration configuration = new Configuration();
        MappedStatement statement = statement(configuration, "known.select");
        configuration.addMappedStatement(statement);
        ExtEnhancer enhancer = new ExtEnhancer(configuration, new ExtContext());

        assertSame(statement, enhancer.getMappedStatement("known.select"));
        assertTrue(enhancer.hasStatement("known.select"));
        assertNull(enhancer.getMappedStatement("missing"));
        assertNull(enhancer.getMappedStatement("missing.select"));
        assertFalse(enhancer.hasStatement("missing.select"));
    }

    @Test
    void resolvesStatementsDeclaredByParentMapperInterfaces() {
        Configuration configuration = new Configuration();
        MappedStatement parentStatement = statement(configuration, ParentMapper.class.getName() + ".findById");
        configuration.addMappedStatement(parentStatement);
        configuration.addMapper(ChildMapper.class);
        ExtEnhancer enhancer = new ExtEnhancer(configuration, new ExtContext());
        String childStatementId = ChildMapper.class.getName() + ".findById";

        assertSame(parentStatement, enhancer.getMappedStatement(childStatementId));
        assertFalse(enhancer.hasStatement(childStatementId));
    }

    @Test
    void buildsAndRegistersStatementsForEnhancedMapperMethods() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:adapter_test;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(EntityMapper.class);
        ExtEnhancer enhancer = new ExtEnhancer(configuration, new ExtContext());
        String statementId = EntityMapper.class.getName() + ".getById";

        MappedStatement statement = enhancer.getMappedStatement(statementId);

        assertSame(statement, configuration.getMappedStatement(statementId));
        assertTrue(enhancer.hasStatement(statementId));
        assertSame(SqlCommandType.SELECT, statement.getSqlCommandType());
        assertDoesNotThrow(enhancer::validateAllMapperMethod);
    }

    private MappedStatement statement(Configuration configuration, String id) {
        return new MappedStatement.Builder(configuration, id, new StaticSqlSource(configuration, "SELECT 1"), SqlCommandType.SELECT).build();
    }

    interface ParentMapper extends ExtMapper<AdapterEntity> {
        AdapterEntity findById(@Param("id") Long id);
    }

    interface ChildMapper extends ParentMapper {
    }

    interface EntityMapper extends ExtMapper<AdapterEntity> {
        AdapterEntity getById(@Param("id") Long id);
    }

    @Table(name = "adapter_entity")
    static class AdapterEntity {
        @Id
        @Column
        private Long id;
        @Column
        private String name;
    }
}
