package io.github.mybatisext.jpa;

import java.util.List;
import java.util.Objects;

import io.github.mybatisext.metadata.PropertyInfo;

public class OrderBy {

    private List<PropertyInfo> propertyInfos;
    private OrderByType type;

    public List<PropertyInfo> getPropertyInfos() {
        return propertyInfos;
    }

    public void setPropertyInfos(List<PropertyInfo> propertyInfos) {
        this.propertyInfos = propertyInfos;
    }

    public OrderByType getType() {
        return type;
    }

    public void setType(OrderByType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderBy orderBy = (OrderBy) o;
        return Objects.equals(propertyInfos, orderBy.propertyInfos) && type == orderBy.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyInfos, type);
    }
}
