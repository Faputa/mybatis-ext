package io.github.mybatisext.jpa;

import io.github.mybatisext.metadata.PropertyInfo;

public class PropertyList {

    private final PropertyInfo propertyInfo;
    private final PropertyList tailList;

    public PropertyList(PropertyInfo propertyInfo) {
        this(propertyInfo, null);
    }

    public PropertyList(PropertyInfo propertyInfo, PropertyList tailList) {
        this.propertyInfo = propertyInfo;
        this.tailList = tailList;
    }

    public PropertyList getTailList() {
        return tailList;
    }

    public PropertyInfo getPropertyInfo() {
        return propertyInfo;
    }
}
