package io.github.mybatisext.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ExtContext;

public class TableInfoFactoryTest {

    @Test
    public void test() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtContext extContext = new ExtContext();
        Configuration configuration = ConfigurationFactory.create(environment, extContext);
        TableInfoFactory tableInfoFactory = new TableInfoFactory(configuration, extContext);
        TableInfo tableInfo = tableInfoFactory.getTableInfo(TablePermission.class);
        assertEquals(5, tableInfo.getAliasToJoinTableInfo().size());
        assertTrue(tableInfo.getNameToPropertyInfo().get("tableId2").isReadonly());
    }
}
