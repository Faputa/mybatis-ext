package io.github.mybatisext.jpa;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import com.mysql.cj.jdbc.MysqlDataSource;

import io.github.mybatisext.ExtConfiguration;
import io.github.mybatisext.ExtContext;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.table.PrivilegeTable;

public class ConditionFactoryTest {

    @Test
    public void testFromCriteria() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabaseName("visual");
        dataSource.setUser("root");
        dataSource.setPassword("root");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtConfiguration configuration = new ExtConfiguration(environment, new ExtContext());
        ConditionList conditionList = ConditionFactory.fromCriteria(configuration, PrivilegeTable.class, "pt");
        System.out.println(conditionList);
    }

    @Test
    public void testFromTableInfo() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabaseName("visual");
        dataSource.setUser("root");
        dataSource.setPassword("root");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtConfiguration configuration = new ExtConfiguration(environment, new ExtContext());
        TableInfo tableInfo = TableInfoFactory.getTableInfo(configuration, PrivilegeTable.class);
        ConditionList conditionList = ConditionFactory.fromTableInfo(tableInfo, false, ConditionTest.NotNull, "pt");
        System.out.println(conditionList);
    }
}
