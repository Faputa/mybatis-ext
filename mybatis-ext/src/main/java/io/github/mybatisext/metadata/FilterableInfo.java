package io.github.mybatisext.metadata;

import java.util.Objects;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.jpa.CompareOperator;
import io.github.mybatisext.jpa.LogicalOperator;

public class FilterableInfo {

    private IfTest test;
    private CompareOperator operator;
    private LogicalOperator logicalOperator;
    private boolean ignorecase;
    private boolean not;
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

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
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
        FilterableInfo that = (FilterableInfo) o;
        return ignorecase == that.ignorecase && not == that.not && test == that.test && operator == that.operator && logicalOperator == that.logicalOperator && Objects.equals(testTemplate, that.testTemplate) && Objects.equals(exprTemplate, that.exprTemplate) && Objects.equals(secondVariable, that.secondVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(test, operator, logicalOperator, ignorecase, not, testTemplate, exprTemplate, secondVariable);
    }
}
