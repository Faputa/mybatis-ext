package io.github.mybatisext.metadata;

import java.util.HashMap;
import java.util.Map;

import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.util.Getter;

public class TableInfo implements Getter<PropertyInfo> {

    private TableName tableName;
    private JavaType classType;
    private JoinGraph joinGraph;
    private JoinTableInfo joinTableInfo;
    private final Map<String, JoinTableInfo> aliasToJoinTableInfo = new HashMap<>();
    private final Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();

    public TableName getTableName() {
        return tableName;
    }

    public void setTableName(TableName tableName) {
        this.tableName = tableName;
    }

    public JavaType getClassType() {
        return classType;
    }

    public void setClassType(JavaType classType) {
        this.classType = classType;
    }

    public JoinGraph getJoinGraph() {
        return joinGraph;
    }

    public void setJoinGraph(JoinGraph joinGraph) {
        this.joinGraph = joinGraph;
    }

    public JoinTableInfo getJoinTableInfo() {
        return joinTableInfo;
    }

    public void setJoinTableInfo(JoinTableInfo joinTableInfo) {
        this.joinTableInfo = joinTableInfo;
    }

    public Map<String, JoinTableInfo> getAliasToJoinTableInfo() {
        return aliasToJoinTableInfo;
    }

    public Map<String, PropertyInfo> getNameToPropertyInfo() {
        return nameToPropertyInfo;
    }

    public String getName() {
        return tableName.toString();
    }

    @Override
    public PropertyInfo get(String key) {
        return nameToPropertyInfo.get(key);
    }

    @Override
    public String toString() {
        return getName();
    }
}
