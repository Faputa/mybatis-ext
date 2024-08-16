package io.github.mybatisext.jpa;

import io.github.mybatisext.metadata.PropertyInfo;

public class Condition {

    private PropertyInfo propertyInfo;
    private boolean ignorecase;
    private boolean not;
    private ConditionRel rel;
    private String variable;
    private String secondVariable;

    private ConditionTest test;
    private ConditionList conditionList;

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

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getSecondVariable() {
        return secondVariable;
    }

    public void setSecondVariable(String secondVariable) {
        this.secondVariable = secondVariable;
    }

    public ConditionTest getTest() {
        return test;
    }

    public void setTest(ConditionTest test) {
        this.test = test;
    }

    public ConditionList getConditionList() {
        return conditionList;
    }

    public void setConditionList(ConditionList conditionList) {
        this.conditionList = conditionList;
    }
}
