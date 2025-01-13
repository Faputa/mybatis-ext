package io.github.mybatisext.jpa;

import java.util.List;
import java.util.Objects;

import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

public class Semantic {

    private final SemanticType type;
    private boolean distinct;
    private boolean ignoreNull;
    private Limit limit;
    private Condition where;
    private List<PropertyInfo> selectItems;
    private List<OrderByElement> orderBy;
    private List<PropertyInfo> groupBy;
    private Condition having;
    private TableInfo tableInfo;
    private Variable parameter;

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

    public List<PropertyInfo> getSelectItems() {
        return selectItems;
    }

    public void setSelectItems(List<PropertyInfo> selectItems) {
        this.selectItems = selectItems;
    }

    public List<OrderByElement> getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(List<OrderByElement> orderBy) {
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

    public Variable getParameter() {
        return parameter;
    }

    public void setParameter(Variable parameter) {
        this.parameter = parameter;
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
        return distinct == semantic.distinct && ignoreNull == semantic.ignoreNull && type == semantic.type && Objects.equals(limit, semantic.limit) && Objects.equals(where, semantic.where) && Objects.equals(selectItems, semantic.selectItems) && Objects.equals(orderBy, semantic.orderBy) && Objects.equals(groupBy, semantic.groupBy) && Objects.equals(having, semantic.having) && Objects.equals(tableInfo, semantic.tableInfo) && Objects.equals(parameter, semantic.parameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, distinct, ignoreNull, limit, where, selectItems, orderBy, groupBy, having, tableInfo, parameter);
    }
}
