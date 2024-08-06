package io.github.mybatisext.jpa;

import io.github.mybatisext.metadata.PropertyInfo;

public class Condition {

    private final Condition prevCondition;
    private PropertyInfo propertyInfo;
    private Boolean ignorecase;
    private Boolean not;
    private ConditionRel rel;
    private String variableA;
    private String variableB;

    public Condition() {
        this(null);
    }

    public Condition(Condition prevCondition) {
        this.prevCondition = prevCondition;
    }

    public Condition getPrevCondition() {
        return prevCondition;
    }

    public PropertyInfo getPropertyInfo() {
        if (propertyInfo != null) {
            return propertyInfo;
        }
        if (prevCondition != null) {
            return prevCondition.getPropertyInfo();
        }
        return null;
    }

    public Condition setPropertyInfo(PropertyInfo propertyInfo) {
        this.propertyInfo = propertyInfo;
        return this;
    }

    public Boolean isIgnorecase() {
        if (ignorecase != null) {
            return ignorecase;
        }
        if (prevCondition != null) {
            return prevCondition.isIgnorecase();
        }
        return false;
    }

    public Condition setIgnorecase(Boolean ignorecase) {
        this.ignorecase = ignorecase;
        return this;
    }

    public Boolean isNot() {
        if (not != null) {
            return not;
        }
        if (prevCondition != null) {
            return prevCondition.isNot();
        }
        return false;
    }

    public Condition setNot(Boolean not) {
        this.not = not;
        return this;
    }

    public ConditionRel getRel() {
        if (rel != null) {
            return rel;
        }
        if (prevCondition != null) {
            return prevCondition.getRel();
        }
        return null;
    }

    public Condition setRel(ConditionRel rel) {
        this.rel = rel;
        return this;
    }

    public String getVariableA() {
        if (variableA != null) {
            return variableA;
        }
        if (prevCondition != null) {
            return prevCondition.getVariableA();
        }
        return null;
    }

    public Condition setVariableA(String variableA) {
        this.variableA = variableA;
        return this;
    }

    public String getVariableB() {
        if (variableB != null) {
            return variableB;
        }
        if (prevCondition != null) {
            return prevCondition.getVariableB();
        }
        return null;
    }

    public Condition setVariableB(String variableB) {
        this.variableB = variableB;
        return this;
    }
}
