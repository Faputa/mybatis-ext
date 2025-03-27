package io.github.mybatisext.jpa;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ExtContext;

public class HasTypeHandlerTest {

    @Test
    void test() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = ConfigurationFactory.create(environment, new ExtContext());
        System.out.println(configuration.getTypeHandlerRegistry().hasTypeHandler(Map.class));
        System.out.println(configuration.getTypeHandlerRegistry().hasTypeHandler(Set.class));
        System.out.println(configuration.getTypeHandlerRegistry().hasTypeHandler(List.class));
    }
}
