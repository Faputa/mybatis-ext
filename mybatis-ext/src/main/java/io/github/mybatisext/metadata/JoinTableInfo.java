package io.github.mybatisext.metadata;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import io.github.mybatisext.util.StringUtils;

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

    public LinkedHashSet<String> getTableAliases() {
        LinkedHashSet<String> tableAliases = new LinkedHashSet<>();
        leftJoinTableInfos.forEach((leftJoinColumn, leftJoinTable) -> {
            tableAliases.addAll(leftJoinTable.getTableAliases());
        });
        tableAliases.add(alias);
        return tableAliases;
    }

    @Override
    public String toString() {
        return (tableInfo != null ? tableInfo.getName() : "") + " AS " + alias;
    }
}
