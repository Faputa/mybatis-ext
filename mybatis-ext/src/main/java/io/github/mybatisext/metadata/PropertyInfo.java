package io.github.mybatisext.metadata;

import java.util.HashMap;
import java.util.Objects;

import org.apache.ibatis.type.JdbcType;

import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.annotation.LoadType;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.util.StringUtils;

public class PropertyInfo extends HashMap<String, PropertyInfo> {

    private final String name;
    private final String prefix;
    private JoinTableInfo joinTableInfo;
    private GenericType javaType;
    private JdbcType jdbcType;
    private boolean ownColumn;
    private boolean readonly;
    private FilterableInfo filterableInfo;
    // 如果是简单类型属性
    private String columnName;

    // resultMap项的类型
    private ResultType resultType;

    // resultType=ID
    private IdType idType;
    private Class<?> customIdGenerator;

    private LoadType loadType;
    // resultType=COLLECTION
    private GenericType ofType;

    public PropertyInfo(String name) {
        this("", name);
    }

    public PropertyInfo(String prefix, String name) {
        this.name = name;
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getFullName() {
        return StringUtils.isNotBlank(prefix) ? prefix + "." + name : name;
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

    public FilterableInfo getFilterableInfo() {
        return filterableInfo;
    }

    public void setFilterableInfo(FilterableInfo filterableInfo) {
        this.filterableInfo = filterableInfo;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
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

    public Class<?> getCustomIdGenerator() {
        return customIdGenerator;
    }

    public void setCustomIdGenerator(Class<?> customIdGenerator) {
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
        return ownColumn == that.ownColumn && readonly == that.readonly && Objects.equals(name, that.name) && Objects.equals(prefix, that.prefix) && Objects.equals(joinTableInfo, that.joinTableInfo) && Objects.equals(javaType, that.javaType) && jdbcType == that.jdbcType && Objects.equals(filterableInfo, that.filterableInfo) && Objects.equals(columnName, that.columnName) && resultType == that.resultType && idType == that.idType && Objects.equals(customIdGenerator, that.customIdGenerator) && loadType == that.loadType && Objects.equals(ofType, that.ofType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, prefix, joinTableInfo, javaType, jdbcType, ownColumn, readonly, filterableInfo, columnName, resultType, idType, customIdGenerator, loadType, ofType);
    }

    @Override
    public String toString() {
        return joinTableInfo.getAlias() + "." + columnName;
    }
}
