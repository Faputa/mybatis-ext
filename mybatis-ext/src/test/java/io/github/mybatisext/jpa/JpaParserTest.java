package io.github.mybatisext.jpa;

import java.lang.reflect.Method;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import com.mysql.cj.jdbc.MysqlDataSource;

import io.github.mybatisext.ExtConfiguration;
import io.github.mybatisext.ExtContext;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.table.PrivilegeTable;

public class JpaParserTest {
    public static void main(String[] args) {
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

        JpaParser jpaParser = new JpaParser();
        for (Method method : JpaParserExample.class.getDeclaredMethods()) {
            Semantic semantic = jpaParser.parse(tableInfo, method.getName(), method.getParameters());
            System.out.println(semantic);
        }
    }
}
