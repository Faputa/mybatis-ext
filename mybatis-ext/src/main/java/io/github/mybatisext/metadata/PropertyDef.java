package io.github.mybatisext.metadata;

import java.util.Map;

import org.apache.ibatis.type.JdbcType;

import io.github.mybatisext.model.JavaAnnotatedElement;
import io.github.mybatisext.model.JavaAnnotation;
import io.github.mybatisext.model.JavaType;

public class PropertyDef {

    private JavaAnnotatedElement source;
    private JavaAnnotatedElement mappingSource;
    private JavaType sourceType;
    private String name;
    private JavaType classType;
    private JavaType declaringType;
    private JdbcType jdbcType;
    private boolean ownColumn;
    private boolean readonly;
    // 只处理@Column注解的属性
    private String columnName;
    private JavaAnnotation id;
    private JavaAnnotation[] joinRelations;
    private JavaAnnotation filterable;
    private JavaAnnotation fetch;
    private JavaAnnotation cascade;
    private Map<String, PropertyDef> nameToPropertyDef;

    public JavaAnnotatedElement getSource() {
        return source;
    }

    public void setSource(JavaAnnotatedElement source) {
        this.source = source;
    }

    public JavaAnnotatedElement getMappingSource() {
        return mappingSource;
    }

    public void setMappingSource(JavaAnnotatedElement mappingSource) {
        this.mappingSource = mappingSource;
    }

    public JavaType getSourceType() {
        return sourceType;
    }

    public void setSourceType(JavaType sourceType) {
        this.sourceType = sourceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JavaType getClassType() {
        return classType;
    }

    public void setClassType(JavaType classType) {
        this.classType = classType;
    }

    public JavaType getDeclaringType() {
        return declaringType;
    }

    public void setDeclaringType(JavaType declaringType) {
        this.declaringType = declaringType;
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

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public JavaAnnotation getId() {
        return id;
    }

    public void setId(JavaAnnotation id) {
        this.id = id;
    }

    public JavaAnnotation[] getJoinRelations() {
        return joinRelations;
    }

    public void setJoinRelations(JavaAnnotation[] joinRelations) {
        this.joinRelations = joinRelations;
    }

    public JavaAnnotation getFilterable() {
        return filterable;
    }

    public void setFilterable(JavaAnnotation filterable) {
        this.filterable = filterable;
    }

    public JavaAnnotation getFetch() {
        return fetch;
    }

    public void setFetch(JavaAnnotation fetch) {
        this.fetch = fetch;
    }

    public JavaAnnotation getCascade() {
        return cascade;
    }

    public void setCascade(JavaAnnotation cascade) {
        this.cascade = cascade;
    }

    public Map<String, PropertyDef> getNameToPropertyDef() {
        return nameToPropertyDef;
    }

    public void setNameToPropertyDef(Map<String, PropertyDef> nameToPropertyDef) {
        this.nameToPropertyDef = nameToPropertyDef;
    }

    @Override
    public String toString() {
        return name;
    }
}
