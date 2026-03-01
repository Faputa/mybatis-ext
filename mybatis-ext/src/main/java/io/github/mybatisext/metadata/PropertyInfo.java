package io.github.mybatisext.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.ibatis.mapping.FetchType;
import org.apache.ibatis.type.JdbcType;

import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.util.Getter;

public class PropertyInfo implements Getter<PropertyInfo> {

    private String name;
    private String fullName;
    private JoinTableInfo joinTableInfo;
    private GenericType javaType;
    private JdbcType jdbcType;
    private boolean ownColumn;
    private boolean readonly;
    // 注解Filterable或者开启defaultFilterable选项
    private FilterableInfo filterableInfo;
    // 如果是简单类型属性
    private String columnName;
    // resultMap项的类型
    private PropertyType propertyType;
    // resultType=ID
    private IdType idType;
    private Class<?> customIdGenerator;
    // resultType=COLLECTION
    private GenericType ofType;
    private FetchType fetchType;
    private final Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
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

    public GenericType getOfType() {
        return ofType;
    }

    public void setOfType(GenericType ofType) {
        this.ofType = ofType;
    }

    public FetchType getFetchType() {
        return fetchType;
    }

    public void setFetchType(FetchType fetchType) {
        this.fetchType = fetchType;
    }

    public Map<String, PropertyInfo> getNameToPropertyInfo() {
        return nameToPropertyInfo;
    }

    @Override
    public PropertyInfo get(String key) {
        return nameToPropertyInfo.get(key);
    }

    public void collectUsedJoinTableInfo(Collection<JoinTableInfo> joinTableInfos) {
        joinTableInfos.add(joinTableInfo);
        for (PropertyInfo propertyInfo : nameToPropertyInfo.values()) {
            propertyInfo.collectUsedJoinTableInfo(joinTableInfos);
        }
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
        return Objects.equals(fullName, that.fullName) && Objects.equals(columnName, that.columnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName, columnName);
    }

    @Override
    public String toString() {
        if (!nameToPropertyInfo.isEmpty()) {
            return nameToPropertyInfo.toString();
        }
        return joinTableInfo.getAlias() + "." + columnName;
    }
}
