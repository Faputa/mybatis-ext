package io.github.mybatisext.resultmap;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import com.mysql.cj.jdbc.MysqlDataSource;

import io.github.mybatisext.ExtConfiguration;
import io.github.mybatisext.ExtContext;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.table.PrivilegeTable;

public class ResultMapBuilderTest {

    @Test
    public void test() {
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
        ResultMap resultMap = new ResultMapBuilder(configuration).buildResultMap(tableInfo);
        System.out.println(resultMap);
    }
}
