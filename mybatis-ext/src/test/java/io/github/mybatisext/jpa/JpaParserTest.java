package io.github.mybatisext.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.dialect.H2Dialect;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.metadata.TablePermission;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.statement.ParameterSignature;
import io.github.mybatisext.statement.ParameterSignatureHelper;
import io.github.mybatisext.statement.SemanticScriptHelper;

public class JpaParserTest {

    private final Configuration configuration;
    private final TableInfo tableInfo;
    private final JpaParser jpaParser;

    JpaParserTest() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtContext extContext = new ExtContext();
        configuration = ConfigurationFactory.create(environment, extContext);
        TableInfoFactory tableInfoFactory = new TableInfoFactory(configuration, extContext);
        tableInfo = tableInfoFactory.getTableInfo(TablePermission.class);
        jpaParser = new JpaParser(configuration, tableInfoFactory);
    }

    @Test
    public void testParse() {
        GenericType genericType = GenericTypeFactory.build(JpaParserExample.class);
        Map<String, Map<Semantic, String>> map = new HashMap<>();
        for (GenericMethod method : genericType.getMethods()) {
            Semantic semantic = jpaParser.parse(tableInfo, method.getName(), method.getParameters(), method.getGenericReturnType());
            ParameterSignature parameterSignature = ParameterSignatureHelper.buildParameterSignature(configuration, method);
            String s = ParameterSignatureHelper.toString(parameterSignature);
            map.computeIfAbsent(method.getName(), k -> new HashMap<>()).put(semantic, s);
        }
        Dialect dialect = new H2Dialect();
        map.forEach((k, v) -> {
            String script = SemanticScriptHelper.buildScript(v.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)), dialect);
            System.out.println(k + "================================" + v.size());
            System.out.println(script);
            System.out.println();
        });
    }
}
