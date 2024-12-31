package io.github.mybatisext.jpa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.mybatisext.metadata.PropertyInfo;

public class ConditionHook {

    private final PropertyInfo propertyInfo;
    private final Set<String> usedTableAliases = new HashSet<>();
    private final Map<String, PropertyInfo> propertyInfos = new HashMap<String, PropertyInfo>() {
        @Override
        public PropertyInfo get(Object key) {
            PropertyInfo propertyInfo = super.get(key);
            if (propertyInfo != null) {
                usedTableAliases.add(propertyInfo.getJoinTableInfo().getAlias());
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
            usedTableAliases.add(propertyInfo.getJoinTableInfo().getAlias());
        }
        return propertyInfo;
    }

    public Set<String> getUsedTableAliases() {
        return usedTableAliases;
    }

    public Map<String, PropertyInfo> getPropertyInfos() {
        return propertyInfos;
    }
}
