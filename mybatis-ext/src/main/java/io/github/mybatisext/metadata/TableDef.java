package io.github.mybatisext.metadata;

import java.util.Map;

import io.github.mybatisext.reflect.GenericType;

public class TableDef {

    private GenericType classType;
    private GenericType tableType;
    private Map<String, PropertyDef> nameToPropertyDef;
    private TableName tableName;

    public GenericType getClassType() {
        return classType;
    }

    public void setClassType(GenericType classType) {
        this.classType = classType;
    }

    public GenericType getTableType() {
        return tableType;
    }

    public void setTableType(GenericType tableType) {
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
