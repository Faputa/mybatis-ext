package io.github.mybatisext.jpa;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import com.mysql.cj.jdbc.MysqlDataSource;

import io.github.mybatisext.ExtConfiguration;
import io.github.mybatisext.ExtContext;

public class HasTypeHandlerTest {

    @Test
    void test() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabaseName("visual");
        dataSource.setUser("root");
        dataSource.setPassword("root");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtConfiguration configuration = new ExtConfiguration(environment, new ExtContext());
        System.out.println(configuration.getTypeHandlerRegistry().hasTypeHandler(Map.class));
        System.out.println(configuration.getTypeHandlerRegistry().hasTypeHandler(Set.class));
        System.out.println(configuration.getTypeHandlerRegistry().hasTypeHandler(List.class));
    }
}
