package io.github.mybatisext.jpa;

import java.util.List;

public class Semantic {

    private final SemanticType type;
    private boolean distinct;
    private boolean ignoreNull;
    private Limit limit;
    private ConditionList conditionList;
    private OrderBy orderBy;
    private GroupBy groupBy;

    public Semantic(SemanticType type) {
        this.type = type;
    }

    public SemanticType getType() {
        return type;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isIgnoreNull() {
        return ignoreNull;
    }

    public void setIgnoreNull(boolean ignoreNull) {
        this.ignoreNull = ignoreNull;
    }

    public Limit getLimit() {
        return limit;
    }

    public void setLimit(Limit limit) {
        this.limit = limit;
    }

    public ConditionList getConditionList() {
        return conditionList;
    }

    public void setConditionList(ConditionList conditionList) {
        this.conditionList = conditionList;
    }

    public OrderBy getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
    }

    public GroupBy getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(GroupBy groupBy) {
        this.groupBy = groupBy;
    }

    public void setModifierList(List<Modifier> modifiers) {
        for (Modifier modifier : modifiers) {
            modifier.accept(this);
        }
    }
}
