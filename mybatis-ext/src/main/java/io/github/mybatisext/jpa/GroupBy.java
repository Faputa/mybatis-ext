package io.github.mybatisext.jpa;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GroupBy groupBy = (GroupBy) o;
        return Objects.equals(propertyInfos, groupBy.propertyInfos);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(propertyInfos);
    }
}
