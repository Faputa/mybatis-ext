package io.github.mybatisext.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import com.mysql.cj.jdbc.MysqlDataSource;

import io.github.mybatisext.adapter.ExtConfiguration;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.table.PrivilegeTable;

public class ConditionHelperTest {

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
        Condition condition = ConditionHelper.fromTableInfo(tableInfo, false, "pt");
        System.out.println(condition);
    }

    @TableRef(PrivilegeTable.class)
    static class A {
    }

    @TableRef(PrivilegeTable.class)
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
