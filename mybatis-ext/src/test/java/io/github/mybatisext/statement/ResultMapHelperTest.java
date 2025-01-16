package io.github.mybatisext.statement;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ExtConfiguration;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.dialect.H2Dialect;
import io.github.mybatisext.metadata.TablePermission;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class ResultMapHelperTest {

    @Test
    public void test() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtConfiguration configuration = new ExtConfiguration(environment, new ExtContext());
        ExtContext extContext = new ExtContext();
        MappedStatementHelper mappedStatementHelper = new MappedStatementHelper(configuration, extContext);
        ResultMapHelper resultMapHelper = new ResultMapHelper(configuration, mappedStatementHelper);
        GenericType returnType = GenericTypeFactory.build(TablePermission.class);
        ResultMap resultMap = resultMapHelper.buildResultMap(returnType, new H2Dialect(), false);
        System.out.println(resultMap);
    }
}
