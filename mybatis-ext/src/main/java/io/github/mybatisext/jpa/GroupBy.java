package io.github.mybatisext.jpa;

import java.util.List;

import io.github.mybatisext.metadata.PropertyInfo;

public class GroupBy implements Modifier {

    private List<PropertyInfo> propertyInfos;

    public List<PropertyInfo> getPropertyInfos() {
        return propertyInfos;
    }

    public void setPropertyInfos(List<PropertyInfo> propertyInfos) {
        this.propertyInfos = propertyInfos;
    }

    @Override
    public void accept(Semantic semantic) {
        semantic.setGroupBy(this);
    }
}
