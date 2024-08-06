package io.github.mybatisext.jpa;

public class OrderBy {

    private final PropertyList propertyList;
    private OrderByType type;

    public OrderBy(PropertyList propertyList) {
        this.propertyList = propertyList;
    }

    public PropertyList getPropertyList() {
        return propertyList;
    }

    public OrderByType getType() {
        return type;
    }

    public void setType(OrderByType type) {
        this.type = type;
    }
}
