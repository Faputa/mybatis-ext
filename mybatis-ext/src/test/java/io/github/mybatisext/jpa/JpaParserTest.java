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

import io.github.mybatisext.adapter.ExtConfiguration;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.dialect.OracleDialect;
import io.github.mybatisext.exception.MybatisExtException;
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
        }
        map.forEach((k, v) -> {
            assertEquals(1, v.size(), k);
            String script = buildScript(v.iterator().next());
            System.out.println(k + "================================");
            System.out.println(script);
            System.out.println();
        });
    }

    private String buildScript(Semantic semantic) {
        Dialect dialect = new OracleDialect();
        if (semantic.getType() == SemanticType.COUNT) {
            return dialect.count(semantic.getTableInfo(), semantic.getWhere());
        }
        if (semantic.getType() == SemanticType.EXISTS) {
            return dialect.exists(semantic.getTableInfo(), semantic.getWhere());
        }
        if (semantic.getType() == SemanticType.SELECT) {
            return dialect.select(semantic.getTableInfo(), semantic.getWhere(), semantic.isDistinct(), semantic.getOrderBy(), semantic.getGroupBy(), semantic.getHaving(), semantic.getLimit());
        }
        if (semantic.getType() == SemanticType.DELETE) {
            return dialect.delete(semantic.getTableInfo(), semantic.getParameter(), semantic.getWhere());
        }
        if (semantic.getType() == SemanticType.INSERT) {
            return dialect.insert(semantic.getTableInfo(), semantic.getParameter(), semantic.isIgnoreNull());
        }
        if (semantic.getType() == SemanticType.UPDATE) {
            return dialect.update(semantic.getTableInfo(), semantic.getParameter(), semantic.getWhere(), semantic.isIgnoreNull());
        }
        throw new MybatisExtException("Unsupported semantic type: " + semantic.getType());
    }
}
