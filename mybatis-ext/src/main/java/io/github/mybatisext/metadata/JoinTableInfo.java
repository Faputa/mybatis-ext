package io.github.mybatisext.metadata;

import java.util.HashMap;
import java.util.Map;

public class JoinTableInfo {

    private TableInfo tableInfo;
    private final Map<JoinColumnInfo, JoinTableInfo> leftJoinTableInfos = new HashMap<>();
    private final Map<JoinColumnInfo, JoinTableInfo> rightJoinTableInfos = new HashMap<>();
    private String alias;
    private boolean merged;

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public void setTableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Map<JoinColumnInfo, JoinTableInfo> getLeftJoinTableInfos() {
        return leftJoinTableInfos;
    }

    public Map<JoinColumnInfo, JoinTableInfo> getRightJoinTableInfos() {
        return rightJoinTableInfos;
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    @Override
    public String toString() {
        return tableInfo.getName() + " AS " + alias;
    }
}
