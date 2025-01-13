package io.github.mybatisext.metadata;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.jpa.CompareOperator;

import java.util.Objects;

public class FilterSpecInfo {

    private IfTest test;
    private CompareOperator operator;
    private String testTemplate;
    private String exprTemplate;
    private String secondVariable;

    public IfTest getTest() {
        return test;
    }

    public void setTest(IfTest test) {
        this.test = test;
    }

    public CompareOperator getOperator() {
        return operator;
    }

    public void setOperator(CompareOperator operator) {
        this.operator = operator;
    }

    public String getTestTemplate() {
        return testTemplate;
    }

    public void setTestTemplate(String testTemplate) {
        this.testTemplate = testTemplate;
    }

    public String getExprTemplate() {
        return exprTemplate;
    }

    public void setExprTemplate(String exprTemplate) {
        this.exprTemplate = exprTemplate;
    }

    public String getSecondVariable() {
        return secondVariable;
    }

    public void setSecondVariable(String secondVariable) {
        this.secondVariable = secondVariable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilterSpecInfo that = (FilterSpecInfo) o;
        return test == that.test && operator == that.operator && Objects.equals(testTemplate, that.testTemplate) && Objects.equals(exprTemplate, that.exprTemplate) && Objects.equals(secondVariable, that.secondVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(test, operator, testTemplate, exprTemplate, secondVariable);
    }
}
