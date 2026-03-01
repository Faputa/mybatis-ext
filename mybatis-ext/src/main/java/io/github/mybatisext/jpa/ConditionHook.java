package io.github.mybatisext.jpa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.mybatisext.metadata.JoinTableInfo;
import io.github.mybatisext.metadata.PropertyInfo;

public class ConditionHook {

    private final PropertyInfo propertyInfo;
    private final Set<JoinTableInfo> joinTableInfos = new HashSet<>();
    private final Map<String, PropertyInfo> propertyInfos = new HashMap<String, PropertyInfo>() {
        @Override
        public PropertyInfo get(Object key) {
            PropertyInfo propertyInfo = super.get(key);
            if (propertyInfo != null) {
                joinTableInfos.add(propertyInfo.getJoinTableInfo());
            }
            return propertyInfo;
        }
    };

    public ConditionHook(Condition condition) {
        this.propertyInfo = condition.getPropertyInfo();
        this.propertyInfos.putAll(condition.getPropertyInfos());
    }

    public PropertyInfo getPropertyInfo() {
        if (propertyInfo != null) {
            joinTableInfos.add(propertyInfo.getJoinTableInfo());
        }
        return propertyInfo;
    }

    public Set<JoinTableInfo> getUsedJoinTableInfos() {
        return joinTableInfos;
    }

    public Map<String, PropertyInfo> getPropertyInfos() {
        return propertyInfos;
    }
}
