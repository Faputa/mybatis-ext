package io.github.mybatisext.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import com.mysql.cj.jdbc.MysqlDataSource;

import io.github.mybatisext.ExtConfiguration;
import io.github.mybatisext.ExtContext;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.table.PrivilegeTable;

public class JpaParserTest {

    final ExtConfiguration configuration;
    final TableInfo tableInfo;

    JpaParserTest() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabaseName("visual");
        dataSource.setUser("root");
        dataSource.setPassword("root");
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        configuration = new ExtConfiguration(environment, new ExtContext());
        tableInfo = TableInfoFactory.getTableInfo(configuration, PrivilegeTable.class);
    }

    @Test
    public void testParse() {
        JpaParser jpaParser = new JpaParser();
        GenericType genericType = GenericTypeFactory.build(JpaParserExample.class);
        Map<String, Set<Semantic>> map = new HashMap<>();
        for (GenericMethod method : genericType.getMethods()) {
            Semantic semantic = jpaParser.parse(configuration, tableInfo, method.getName(), method.getParameters());
            map.computeIfAbsent(method.getName(), k -> new HashSet<>()).add(semantic);
            System.out.println(semantic);
        }
        map.forEach((k, v) -> {
            assertEquals(1, v.size(), k);
        });
    }
}
