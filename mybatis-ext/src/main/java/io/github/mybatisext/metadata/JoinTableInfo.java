package io.github.mybatisext.metadata;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class JoinTableInfo {

    private TableInfo tableInfo;
    private final Map<JoinColumnInfo, JoinTableInfo> leftJoinTableInfos = new HashMap<>();
    private final Map<JoinColumnInfo, JoinTableInfo> rightJoinTableInfos = new HashMap<>();
    private String alias;

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

    public void collectTableAliases(LinkedHashSet<String> tableAliases) {
        leftJoinTableInfos.forEach((leftJoinColumn, leftJoinTable) -> {
            leftJoinTable.collectTableAliases(tableAliases);
        });
        tableAliases.add(alias);
    }

    @Override
    public String toString() {
        return (tableInfo != null ? tableInfo.getName() : "") + " AS " + alias;
    }
}
