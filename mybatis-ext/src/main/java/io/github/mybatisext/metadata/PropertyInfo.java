package io.github.mybatisext.metadata;

import java.util.ArrayList;
import java.util.List;

public class PropertyInfo {

    private String name;
    private TableInfo tableInfo;
    private ColumnInfo columnInfo;
    private boolean isCollection;
    private String javaType;
    private List<String> tableAliases = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public void setTableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    public ColumnInfo getColumnInfo() {
        return columnInfo;
    }

    public void setColumnInfo(ColumnInfo columnInfo) {
        this.columnInfo = columnInfo;
    }

    public boolean isCollection() {
        return isCollection;
    }

    public void setCollection(boolean isCollection) {
        this.isCollection = isCollection;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public List<String> getTableAliases() {
        return tableAliases;
    }

    public void setTableAliases(List<String> tableAliases) {
        this.tableAliases = tableAliases;
    }
}
