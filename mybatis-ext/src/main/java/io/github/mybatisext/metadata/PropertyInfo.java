package io.github.mybatisext.metadata;

import java.util.LinkedHashSet;

import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.annotation.LoadType;
import io.github.mybatisext.idgenerator.IdGenerator;
import io.github.mybatisext.resultmap.ResultType;

public class PropertyInfo {

    private String name;
    private TableInfo tableInfo;
    private Class<?> javaType;
    private ResultType resultType;

    // resultType=ID,RESULT
    private ColumnInfo columnInfo;

    // resultType=ID
    private IdType idType;
    private IdGenerator<?> customIdGenerator;

    // resultType=COLLECTION
    private Class<?> ofType;

    // resultType=ASSOCIATION,COLLECTION
    private LoadType loadType;
    // 依赖的其他表的别名
    private LinkedHashSet<String> tableAliases = new LinkedHashSet<>();
    private String columnName;

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

    public Class<?> getJavaType() {
        return javaType;
    }

    public void setJavaType(Class<?> javaType) {
        this.javaType = javaType;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public void setResultType(ResultType resultType) {
        this.resultType = resultType;
    }

    public ColumnInfo getColumnInfo() {
        return columnInfo;
    }

    public void setColumnInfo(ColumnInfo columnInfo) {
        this.columnInfo = columnInfo;
    }

    public IdType getIdType() {
        return idType;
    }

    public void setIdType(IdType idType) {
        this.idType = idType;
    }

    public IdGenerator<?> getCustomIdGenerator() {
        return customIdGenerator;
    }

    public void setCustomIdGenerator(IdGenerator<?> customIdGenerator) {
        this.customIdGenerator = customIdGenerator;
    }

    public Class<?> getOfType() {
        return ofType;
    }

    public void setOfType(Class<?> ofType) {
        this.ofType = ofType;
    }

    public LoadType getLoadType() {
        return loadType;
    }

    public void setLoadType(LoadType loadType) {
        this.loadType = loadType;
    }

    public LinkedHashSet<String> getTableAliases() {
        return tableAliases;
    }

    public void setTableAliases(LinkedHashSet<String> tableAliases) {
        this.tableAliases = tableAliases;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
}
