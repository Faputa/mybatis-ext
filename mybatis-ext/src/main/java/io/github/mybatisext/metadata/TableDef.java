package io.github.mybatisext.metadata;

import java.util.Map;

import io.github.mybatisext.model.JavaType;

public class TableDef {

    private JavaType classType;
    private JavaType tableType;
    private Map<String, PropertyDef> nameToPropertyDef;
    private TableName tableName;

    public JavaType getClassType() {
        return classType;
    }

    public void setClassType(JavaType classType) {
        this.classType = classType;
    }

    public JavaType getTableType() {
        return tableType;
    }

    public void setTableType(JavaType tableType) {
        this.tableType = tableType;
    }

    public Map<String, PropertyDef> getNameToPropertyDef() {
        return nameToPropertyDef;
    }

    public void setNameToPropertyDef(Map<String, PropertyDef> nameToPropertyDef) {
        this.nameToPropertyDef = nameToPropertyDef;
    }

    public TableName getTableName() {
        return tableName;
    }

    public void setTableName(TableName tableName) {
        this.tableName = tableName;
    }

    @Override
    public String toString() {
        return classType.getName();
    }
}
