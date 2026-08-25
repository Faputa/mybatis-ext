package io.github.mybatisext.statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.dialect.H2Dialect;
import io.github.mybatisext.fixture.permission.TablePermission;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class ResultMapHelperTest {

    @Test
    public void buildsNestedResultMappings() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtContext extContext = new ExtContext();
        Configuration configuration = new Configuration(environment);
        ResultMapHelper resultMapHelper = new ResultMapHelper(configuration, extContext);
        GenericType returnType = GenericTypeFactory.build(TablePermission.class);
        ResultMap resultMap = resultMapHelper.buildResultMap(returnType, new H2Dialect());
        Map<String, ResultMapping> mappings = resultMap.getResultMappings().stream().collect(Collectors.toMap(ResultMapping::getProperty, Function.identity()));

        assertEquals(TablePermission.class, resultMap.getType());
        assertTrue(resultMap.getIdResultMappings().stream().map(ResultMapping::getProperty).collect(Collectors.toSet()).containsAll(java.util.Arrays.asList("tableId", "roleId")));
        assertNotNull(mappings.get("rowPermissions").getNestedResultMapId());
        assertNotNull(mappings.get("rowPermissions2").getNestedQueryId());
        assertNotNull(mappings.get("columnPermissions").getNestedResultMapId());
    }
}
