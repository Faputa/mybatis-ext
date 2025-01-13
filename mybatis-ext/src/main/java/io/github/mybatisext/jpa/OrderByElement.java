package io.github.mybatisext.jpa;

import java.util.Objects;

import io.github.mybatisext.metadata.PropertyInfo;

public class OrderByElement {

    private PropertyInfo propertyInfo;
    private OrderByType type;

    public PropertyInfo getPropertyInfo() {
        return propertyInfo;
    }

    public void setPropertyInfo(PropertyInfo propertyInfo) {
        this.propertyInfo = propertyInfo;
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
        OrderByElement that = (OrderByElement) o;
        return Objects.equals(propertyInfo, that.propertyInfo) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyInfo, type);
    }
}
