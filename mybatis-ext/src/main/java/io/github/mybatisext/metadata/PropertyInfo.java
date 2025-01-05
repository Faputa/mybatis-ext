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
    private JoinTableInfo joinTableInfo;
    private GenericType javaType;
    private JdbcType jdbcType;
    private boolean ownColumn;
    private boolean readonly;

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

    public boolean isOwnColumn() {
        return ownColumn;
    }

    public void setOwnColumn(boolean ownColumn) {
        this.ownColumn = ownColumn;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PropertyInfo that = (PropertyInfo) o;
        return ownColumn == that.ownColumn && readonly == that.readonly && Objects.equals(name, that.name) && Objects.equals(columnName, that.columnName) && Objects.equals(joinTableInfo, that.joinTableInfo) && Objects.equals(javaType, that.javaType) && jdbcType == that.jdbcType && resultType == that.resultType && idType == that.idType && Objects.equals(customIdGenerator, that.customIdGenerator) && loadType == that.loadType && Objects.equals(ofType, that.ofType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columnName, joinTableInfo, javaType, jdbcType, ownColumn, readonly, resultType, idType, customIdGenerator, loadType, ofType);
    }

    @Override
    public String toString() {
        return joinTableInfo.getAlias() + "." + columnName;
    }
}
