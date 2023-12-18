package io.github.mybatisext.metadata;

import java.util.ArrayList;
import java.util.List;

public class JoinTableInfo {

    private TableInfo tableInfo;
    private List<JoinColumnInfo> leftJoinColumnInfos = new ArrayList<>();
    private List<JoinColumnInfo> rightJoinColumnInfos = new ArrayList<>();
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

    public List<JoinColumnInfo> getLeftJoinColumnInfos() {
        return leftJoinColumnInfos;
    }

    public void setLeftJoinColumnInfos(List<JoinColumnInfo> leftJoinColumnInfos) {
        this.leftJoinColumnInfos = leftJoinColumnInfos;
    }

    public List<JoinColumnInfo> getRightJoinColumnInfos() {
        return rightJoinColumnInfos;
    }

    public void setRightJoinColumnInfos(List<JoinColumnInfo> rightJoinColumnInfos) {
        this.rightJoinColumnInfos = rightJoinColumnInfos;
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }
}
