package io.github.mybatisext.jpa;

import java.util.List;

import io.github.mybatisext.metadata.PropertyInfo;

public class OrderBy implements Modifier {

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
    public void accept(Semantic semantic) {
        semantic.setOrderBy(this);
    }
}
