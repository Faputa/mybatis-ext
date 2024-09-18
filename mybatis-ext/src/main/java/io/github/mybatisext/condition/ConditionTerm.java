package io.github.mybatisext.condition;

import java.util.Objects;

import javax.annotation.Nonnull;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.metadata.PropertyInfo;

public class ConditionTerm implements Condition {

    private PropertyInfo propertyInfo;
    private boolean ignorecase;
    private boolean not;
    private ConditionRel rel;
    private String variable;
    private String secondVariable;
    private IfTest test = IfTest.None;

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

    public IfTest getTest() {
        return test;
    }

    public void setTest(@Nonnull IfTest test) {
        this.test = test;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConditionTerm that = (ConditionTerm) o;
        return ignorecase == that.ignorecase && not == that.not && Objects.equals(propertyInfo, that.propertyInfo) && rel == that.rel && Objects.equals(variable, that.variable) && Objects.equals(secondVariable, that.secondVariable) && test == that.test;
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyInfo, ignorecase, not, rel, variable, secondVariable, test);
    }
}
