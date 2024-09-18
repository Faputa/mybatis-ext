package io.github.mybatisext.jpa;

import java.util.List;
import java.util.Objects;

import io.github.mybatisext.condition.Condition;

public class Semantic {

    private final SemanticType type;
    private boolean distinct;
    private boolean ignoreNull;
    private Limit limit;
    private Condition condition;
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

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Semantic semantic = (Semantic) o;
        return distinct == semantic.distinct && ignoreNull == semantic.ignoreNull && type == semantic.type && Objects.equals(limit, semantic.limit) && Objects.equals(condition, semantic.condition) && Objects.equals(orderBy, semantic.orderBy) && Objects.equals(groupBy, semantic.groupBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, distinct, ignoreNull, limit, condition, orderBy, groupBy);
    }
}
