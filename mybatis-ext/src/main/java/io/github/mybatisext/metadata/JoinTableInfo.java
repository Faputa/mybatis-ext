package io.github.mybatisext.metadata;

import java.util.ArrayList;
import java.util.List;

public class JoinTableInfo {

    TableInfo tableInfo;
    List<JoinColumnInfo> joinColumnInfos = new ArrayList<>();

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public void setTableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    public List<JoinColumnInfo> getJoinColumnInfos() {
        return joinColumnInfos;
    }

    public void setJoinColumnInfos(List<JoinColumnInfo> joinColumnInfos) {
        this.joinColumnInfos = joinColumnInfos;
    }
}
