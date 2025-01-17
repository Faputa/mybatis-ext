package io.github.mybatisext.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ExtConfiguration;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.metadata.TablePermission;

public class ConditionHelperTest {

    @Test
    public void testFromTableInfo() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtContext extContext = new ExtContext();
        ExtConfiguration configuration = new ExtConfiguration(environment, extContext);
        TableInfoFactory tableInfoFactory = new TableInfoFactory(configuration, extContext);
        TableInfo tableInfo = tableInfoFactory.getTableInfo(TablePermission.class);
        Condition condition = ConditionHelper.fromTableInfo(tableInfo, false, "pt");
        System.out.println(condition);
    }

    @TableRef(TablePermission.class)
    static class A {
    }

    @TableRef(TablePermission.class)
    static class B {
    }

    @Test
    public void testAnnotationEquals() {
        TableRef tableRef = A.class.getAnnotation(TableRef.class);
        TableRef tableRef2 = B.class.getAnnotation(TableRef.class);
        Set<TableRef> set = new HashSet<>();
        set.add(tableRef);
        set.add(tableRef2);
        assertEquals(1, set.size());
    }
}
