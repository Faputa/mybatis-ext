package io.github.mybatisext.metadata;

import org.junit.jupiter.api.Test;

import io.github.mybatisext.table.PrivilegeTable;

public class TableInfoFactoryTest {

    @Test
    public void test() {
        TableInfo tableInfo = TableInfoFactory.getTableInfo(PrivilegeTable.class);
        System.out.println(tableInfo);
    }
}
