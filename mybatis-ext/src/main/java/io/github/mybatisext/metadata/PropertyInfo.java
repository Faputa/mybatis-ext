package io.github.mybatisext.metadata;

import java.util.HashMap;
import java.util.Objects;

import org.apache.ibatis.type.JdbcType;

import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.annotation.LoadType;
import io.github.mybatisext.idgenerator.IdGenerator;
import io.github.mybatisext.reflect.GenericType;

public class PropertyInfo extends HashMap<String, PropertyInfo> {

    private String name;
    // 如果是简单类型属性
    private String columnName;
    private TableInfo tableInfo;
    private JoinTableInfo joinTableInfo;
    private GenericType javaType;
    private JdbcType jdbcType;

    // resultMap项的类型
    private ResultType resultType;

    // resultType=ID
    private IdType idType;
    private IdGenerator<?> customIdGenerator;

    private LoadType loadType;
    // resultType=COLLECTION
    private GenericType ofType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public void setTableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    public JoinTableInfo getJoinTableInfo() {
        return joinTableInfo;
    }

    public void setJoinTableInfo(JoinTableInfo joinTableInfo) {
        this.joinTableInfo = joinTableInfo;
    }

    public GenericType getJavaType() {
        return javaType;
    }

    public void setJavaType(GenericType javaType) {
        this.javaType = javaType;
    }

    public JdbcType getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(JdbcType jdbcType) {
        this.jdbcType = jdbcType;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public void setResultType(ResultType resultType) {
        this.resultType = resultType;
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

    public LoadType getLoadType() {
        return loadType;
    }

    public void setLoadType(LoadType loadType) {
        this.loadType = loadType;
    }

    public GenericType getOfType() {
        return ofType;
    }

    public void setOfType(GenericType ofType) {
        this.ofType = ofType;
    }

    public boolean isOwnColumn() {
        return joinTableInfo.getTableInfo() == tableInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PropertyInfo propertyInfo = (PropertyInfo) o;
        return Objects.equals(name, propertyInfo.name) && Objects.equals(tableInfo, propertyInfo.tableInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, tableInfo);
    }

    @Override
    public String toString() {
        return joinTableInfo.getAlias() + "." + columnName;
    }
}
