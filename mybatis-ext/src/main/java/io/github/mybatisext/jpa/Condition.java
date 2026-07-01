package io.github.mybatisext.jpa;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.github.mybatisext.annotation.TestMode;
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
    private Variable collectionVariable;
    private Variable secondVariable;
    private TestMode testMode = TestMode.None;
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
        syncJdbcType();
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
        syncJdbcType();
    }

    public Variable getCollectionVariable() {
        return collectionVariable;
    }

    public void setCollectionVariable(Variable collectionVariable) {
        this.collectionVariable = collectionVariable;
    }

    public Variable getSecondVariable() {
        return secondVariable;
    }

    public void setSecondVariable(Variable secondVariable) {
        this.secondVariable = secondVariable;
        syncJdbcType();
    }

    public TestMode getTestMode() {
        return testMode;
    }

    public void setTestMode(TestMode testMode) {
        this.testMode = testMode;
    }

    public String getTestTemplate() {
        return testTemplate;
    }

    public void setTestTemplate(String testTemplate) {
        this.testTemplate = testTemplate;
    }

    public boolean hasTest() {
        return StringUtils.isNotBlank(testTemplate) || testMode == TestMode.NotEmpty || testMode == TestMode.NotNull;
    }

    public String getExprTemplate() {
        return exprTemplate;
    }

    public void setExprTemplate(String exprTemplate) {
        this.exprTemplate = exprTemplate;
    }

    private void syncJdbcType() {
        if (propertyInfo == null) {
            return;
        }
        if (variable != null) {
            variable.setJdbcType(propertyInfo.getJdbcType());
        }
        if (secondVariable != null) {
            secondVariable.setJdbcType(propertyInfo.getJdbcType());
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
        Condition condition = (Condition) o;
        return ignorecase == condition.ignorecase && not == condition.not && type == condition.type && Objects.equals(subConditions, condition.subConditions) && logicalOperator == condition.logicalOperator && compareOperator == condition.compareOperator && Objects.equals(propertyInfo, condition.propertyInfo) && Objects.equals(variable, condition.variable) && Objects.equals(secondVariable, condition.secondVariable) && testMode == condition.testMode && Objects.equals(testTemplate, condition.testTemplate) && Objects.equals(exprTemplate, condition.exprTemplate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, subConditions, logicalOperator, compareOperator, propertyInfo, ignorecase, not, variable, secondVariable, testMode, testTemplate, exprTemplate);
    }
}
