package io.github.mybatisext.jpa;

import io.github.mybatisext.metadata.PropertyInfo;

public class Condition {

    private PropertyInfo propertyInfo;
    private boolean ignorecase;
    private boolean not;
    private ConditionRel rel;
    private String variableA;
    private String variableB;

    public PropertyInfo getPropertyInfo() {
        return propertyInfo;
    }

    public void setPropertyInfo(PropertyInfo propertyInfo) {
        this.propertyInfo = propertyInfo;
    }

    public boolean isIgnorecase() {
        return ignorecase;
    }

    public void setIgnorecase(boolean ignorecase) {
        this.ignorecase = ignorecase;
    }

    public boolean isNot() {
        return not;
    }

    public void setNot(boolean not) {
        this.not = not;
    }

    public ConditionRel getRel() {
        return rel;
    }

    public void setRel(ConditionRel rel) {
        this.rel = rel;
    }

    public String getVariableA() {
        return variableA;
    }

    public void setVariableA(String variableA) {
        this.variableA = variableA;
    }

    public String getVariableB() {
        return variableB;
    }

    public void setVariableB(String variableB) {
        this.variableB = variableB;
    }
}
