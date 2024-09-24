package io.github.mybatisext.jpa;

import io.github.mybatisext.condition.Condition;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

import java.util.List;
import java.util.Objects;

public class Semantic {

    private final SemanticType type;
    private boolean distinct;
    private boolean ignoreNull;
    private Limit limit;
    private Condition where;
    private OrderBy orderBy;
    private List<PropertyInfo> groupBy;
    private Condition having;
    private TableInfo tableInfo;
    private Variable targetVariable;

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

    public Condition getWhere() {
        return where;
    }

    public void setWhere(Condition where) {
        this.where = where;
    }

    public OrderBy getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
    }

    public List<PropertyInfo> getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(List<PropertyInfo> groupBy) {
        this.groupBy = groupBy;
    }

    public Condition getHaving() {
        return having;
    }

    public void setHaving(Condition having) {
        this.having = having;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public void setTableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    public Variable getTargetVariable() {
        return targetVariable;
    }

    public void setTargetVariable(Variable targetVariable) {
        this.targetVariable = targetVariable;
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
        return distinct == semantic.distinct && ignoreNull == semantic.ignoreNull && type == semantic.type && Objects.equals(limit, semantic.limit) && Objects.equals(where, semantic.where) && Objects.equals(orderBy, semantic.orderBy) && Objects.equals(groupBy, semantic.groupBy) && Objects.equals(having, semantic.having) && Objects.equals(tableInfo, semantic.tableInfo) && Objects.equals(targetVariable, semantic.targetVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, distinct, ignoreNull, limit, where, orderBy, groupBy, having, tableInfo, targetVariable);
    }
}
