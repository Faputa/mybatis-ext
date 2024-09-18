package io.github.mybatisext.condition;

public class ConditionList {

    private final ConditionTerm condition;
    private final ConditionList tailList;
    private final ConditionCompRel rel;

    public ConditionList(ConditionTerm condition) {
        this(condition, null, null);
    }

    public ConditionList(ConditionTerm condition, ConditionList tailList, ConditionCompRel rel) {
        this.condition = condition;
        this.tailList = tailList;
        this.rel = rel;
    }

    public ConditionTerm getCondition() {
        return condition;
    }

    public ConditionList getTailList() {
        return tailList;
    }

    public ConditionCompRel getRel() {
        return rel;
    }
}
