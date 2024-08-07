package io.github.mybatisext.jpa;

public class Semantic {

    private final Semantic prevSemantic;
    private SemanticType type;
    private Boolean distinct;
    private Boolean ignoreNull;
    private Limit limit;
    private ConditionList conditionList;
    private OrderBy orderBy;
    private GroupBy groupBy;

    public Semantic() {
        this(null);
    }

    public Semantic(Semantic prevSemantic) {
        this.prevSemantic = prevSemantic;
    }

    public Semantic getPrevSemantic() {
        return prevSemantic;
    }

    public SemanticType getType() {
        if (type != null) {
            return type;
        }
        if (prevSemantic != null) {
            return prevSemantic.getType();
        }
        return null;
    }

    public Semantic setType(SemanticType type) {
        this.type = type;
        return this;
    }

    public Boolean isDistinct() {
        if (distinct != null) {
            return distinct;
        }
        if (prevSemantic != null) {
            return prevSemantic.isDistinct();
        }
        return false;
    }

    public Semantic setDistinct(Boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    public Boolean isIgnoreNull() {
        if (ignoreNull != null) {
            return ignoreNull;
        }
        if (prevSemantic != null) {
            return prevSemantic.isIgnoreNull();
        }
        return false;
    }

    public Semantic setIgnoreNull(Boolean ignoreNull) {
        this.ignoreNull = ignoreNull;
        return this;
    }

    public Limit getLimit() {
        if (limit != null) {
            return limit;
        }
        if (prevSemantic != null) {
            return prevSemantic.getLimit();
        }
        return null;
    }

    public Semantic setLimit(Limit limit) {
        this.limit = limit;
        return this;
    }

    public ConditionList getConditionList() {
        if (conditionList != null) {
            return conditionList;
        }
        if (prevSemantic != null) {
            return prevSemantic.getConditionList();
        }
        return null;
    }

    public Semantic setConditionList(ConditionList conditionList) {
        this.conditionList = conditionList;
        return this;
    }

    public OrderBy getOrderBy() {
        if (orderBy != null) {
            return orderBy;
        }
        if (prevSemantic != null) {
            return prevSemantic.getOrderBy();
        }
        return null;
    }

    public Semantic setOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public GroupBy getGroupBy() {
        if (groupBy != null) {
            return groupBy;
        }
        if (prevSemantic != null) {
            return prevSemantic.getGroupBy();
        }
        return null;
    }

    public Semantic setGroupBy(GroupBy groupBy) {
        this.groupBy = groupBy;
        return this;
    }
}
