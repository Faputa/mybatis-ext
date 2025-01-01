package io.github.mybatisext.jpa;

public class ConditionList {

    private final Condition condition;
    private final ConditionList tailList;
    private final LogicalOperator logicalOperator;

    public ConditionList(Condition condition) {
        this(condition, null, null);
    }

    public ConditionList(Condition condition, ConditionList tailList, LogicalOperator logicalOperator) {
        this.condition = condition;
        this.tailList = tailList;
        this.logicalOperator = logicalOperator;
    }

    public Condition getCondition() {
        return condition;
    }

    public ConditionList getTailList() {
        return tailList;
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }
}
