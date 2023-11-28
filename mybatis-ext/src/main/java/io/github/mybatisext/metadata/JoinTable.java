package io.github.mybatisext.metadata;

import java.util.ArrayList;
import java.util.List;

public class JoinTable {

    TableInfo tableInfo;
    List<JoinColumn> joinColumns = new ArrayList<>();

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public void setTableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    public List<JoinColumn> getJoinColumns() {
        return joinColumns;
    }

    public void setJoinColumns(List<JoinColumn> joinColumns) {
        this.joinColumns = joinColumns;
    }
}
