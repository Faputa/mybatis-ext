package io.github.mybatisext.jpa;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.util.StringUtils;

public class Condition {

    private final ConditionType type;
    private final Set<Condition> subConditions = new HashSet<>();
    private LogicalOperator logicalOperator;
    private CompareOperator compareOperator;
    private PropertyInfo propertyInfo;
    private Map<String, PropertyInfo> propertyInfos;
    private boolean ignorecase;
    private boolean not;
    private Variable variable;
    private Variable secondVariable;
    private IfTest test = IfTest.None;
    private String testTemplate;
    private String exprTemplate;

    public Condition(ConditionType type) {
        this.type = type;
    }

    public ConditionType getType() {
        return type;
    }

    public Set<Condition> getSubConditions() {
        return subConditions;
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    public CompareOperator getCompareOperator() {
        return compareOperator;
    }

    public void setCompareOperator(CompareOperator compareOperator) {
        this.compareOperator = compareOperator;
    }

    public PropertyInfo getPropertyInfo() {
        return propertyInfo;
    }

    public void setPropertyInfo(PropertyInfo propertyInfo) {
        this.propertyInfo = propertyInfo;
    }

    public Map<String, PropertyInfo> getPropertyInfos() {
        return propertyInfos;
    }

    public void setPropertyInfos(Map<String, PropertyInfo> propertyInfos) {
        this.propertyInfos = propertyInfos;
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

    public Variable getVariable() {
        return variable;
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    public Variable getSecondVariable() {
        return secondVariable;
    }

    public void setSecondVariable(Variable secondVariable) {
        this.secondVariable = secondVariable;
    }

    public IfTest getTest() {
        return test;
    }

    public void setTest(IfTest test) {
        this.test = test;
    }

    public String getTestTemplate() {
        return testTemplate;
    }

    public void setTestTemplate(String testTemplate) {
        this.testTemplate = testTemplate;
    }

    public boolean hasTest() {
        return StringUtils.isNotBlank(testTemplate) || test == IfTest.NotEmpty || test == IfTest.NotNull;
    }

    public String getExprTemplate() {
        return exprTemplate;
    }

    public void setExprTemplate(String exprTemplate) {
        this.exprTemplate = exprTemplate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Condition condition = (Condition) o;
        return ignorecase == condition.ignorecase && not == condition.not && type == condition.type && Objects.equals(subConditions, condition.subConditions) && logicalOperator == condition.logicalOperator && compareOperator == condition.compareOperator && Objects.equals(propertyInfo, condition.propertyInfo) && Objects.equals(variable, condition.variable) && Objects.equals(secondVariable, condition.secondVariable) && test == condition.test && Objects.equals(testTemplate, condition.testTemplate) && Objects.equals(exprTemplate, condition.exprTemplate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, subConditions, logicalOperator, compareOperator, propertyInfo, ignorecase, not, variable, secondVariable, test, testTemplate, exprTemplate);
    }
}
