package io.github.mybatisext.metadata;

import java.util.Map;

import io.github.mybatisext.annotation.Fetch;
import io.github.mybatisext.annotation.Filterable;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.reflect.GenericType;

public class PropertyDef {

    private String name;
    private GenericType classType;
    private GenericType declaringType;
    private boolean ownColumn;
    private boolean readonly;
    // 只处理@Column注解的属性
    private String columnName;
    private Id id;
    private JoinRelation[] joinRelations;
    private Filterable filterable;
    private Fetch fetch;
    private Map<String, PropertyDef> nameToPropertyDef;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GenericType getClassType() {
        return classType;
    }

    public void setClassType(GenericType classType) {
        this.classType = classType;
    }

    public GenericType getDeclaringType() {
        return declaringType;
    }

    public void setDeclaringType(GenericType declaringType) {
        this.declaringType = declaringType;
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

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public JoinRelation[] getJoinRelations() {
        return joinRelations;
    }

    public void setJoinRelations(JoinRelation[] joinRelations) {
        this.joinRelations = joinRelations;
    }

    public Filterable getFilterable() {
        return filterable;
    }

    public void setFilterable(Filterable filterable) {
        this.filterable = filterable;
    }

    public Fetch getFetch() {
        return fetch;
    }

    public void setFetch(Fetch fetch) {
        this.fetch = fetch;
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
