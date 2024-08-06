package io.github.mybatisext.jpa;

public class ConditionList {

    private final Condition condition;
    private final ConditionList tailList;
    private final ConditionListRel rel;

    public ConditionList(Condition condition) {
        this(condition, null, null);
    }

    public ConditionList(Condition condition, ConditionList tailList, ConditionListRel rel) {
        this.condition = condition;
        this.tailList = tailList;
        this.rel = rel;
    }

    public Condition getCondition() {
        return condition;
    }

    public ConditionList getTailList() {
        return tailList;
    }

    public ConditionListRel getRel() {
        return rel;
    }
}
