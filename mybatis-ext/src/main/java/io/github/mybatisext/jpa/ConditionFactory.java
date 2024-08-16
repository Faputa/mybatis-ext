package io.github.mybatisext.jpa;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.Criteria;
import io.github.mybatisext.annotation.Criterion;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.resultmap.ResultType;
import io.github.mybatisext.util.StringUtils;

public class ConditionFactory {

    private static final Map<Class<?>, Map<String, ConditionList>> fromCriteriaCache = new ConcurrentHashMap<>();

    public static ConditionList fromCriteria(Configuration configuration, Class<?> exampleClass, String param) {
        Map<String, ConditionList> map = fromCriteriaCache.computeIfAbsent(exampleClass, k -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(param, k -> {
            Criteria criteria = exampleClass.getAnnotation(Criteria.class);
            TableInfo tableInfo = TableInfoFactory.getTableInfo(configuration, criteria.table() != void.class ? criteria.table() : exampleClass);
            return processCriteria(criteria, exampleClass, tableInfo, StringUtils.isNotBlank(param) ? param + "." : "");
        });
    }

    private static final Map<TableInfo, Map<Boolean, Map<ConditionTest, Map<String, ConditionList>>>> fromTableInfoCache = new ConcurrentHashMap<>();

    public static ConditionList fromTableInfo(TableInfo tableInfo, boolean onlyId, ConditionTest test, String param) {
        Map<Boolean, Map<ConditionTest, Map<String, ConditionList>>> map = fromTableInfoCache.computeIfAbsent(tableInfo, k -> new ConcurrentHashMap<>());
        Map<ConditionTest, Map<String, ConditionList>> map2 = map.computeIfAbsent(onlyId, k -> new ConcurrentHashMap<>());
        Map<String, ConditionList> map3 = map2.computeIfAbsent(test, k -> new ConcurrentHashMap<>());
        return map3.computeIfAbsent(param, k -> {
            return processTableInfo(tableInfo, onlyId, test, param);
        });
    }

    private static ConditionList processTableInfo(TableInfo tableInfo, boolean onlyId, ConditionTest test, String param) {
        ConditionList conditionList = null;
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            Condition condition = processPropertyInfo(propertyInfo, onlyId, test, StringUtils.isNotBlank(param) ? param + "." : "");
            if (condition == null) {
                continue;
            }
            if (conditionList == null) {
                conditionList = new ConditionList(condition);
            } else {
                conditionList = new ConditionList(condition, conditionList, ConditionListRel.And);
            }
        }
        return conditionList;
    }

    private static @Nullable Condition processPropertyInfo(PropertyInfo propertyInfo, boolean onlyId, ConditionTest test, String prefix) {
        if (onlyId && propertyInfo.getResultType() != ResultType.ID) {
            return null;
        }
        if (propertyInfo.getResultType() == ResultType.ID || propertyInfo.getResultType() == ResultType.RESULT) {
            Condition condition = new Condition();
            condition.setPropertyInfo(propertyInfo);
            condition.setRel(ConditionRel.Equals);
            condition.setVariable(prefix + propertyInfo.getName());
            condition.setTest(test);
            return condition;
        }
        if (propertyInfo.getResultType() == ResultType.ASSOCIATION) {
            ConditionList conditionList = null;
            for (PropertyInfo subPropertyInfo : propertyInfo.getSubPropertyInfos()) {
                Condition subCondition = processPropertyInfo(subPropertyInfo, onlyId, test, prefix + propertyInfo.getName() + ".");
                if (conditionList == null) {
                    conditionList = new ConditionList(subCondition);
                } else {
                    conditionList = new ConditionList(subCondition, conditionList, ConditionListRel.And);
                }
            }
            Condition condition = new Condition();
            condition.setTest(test);
            condition.setConditionList(conditionList);
            return condition;
        }
        if (propertyInfo.getResultType() == ResultType.COLLECTION) {
            ConditionList conditionList = null;
            for (PropertyInfo subPropertyInfo : propertyInfo.getSubPropertyInfos()) {
                Condition subCondition = processPropertyInfo(subPropertyInfo, onlyId, test, prefix + propertyInfo.getName() + "[0].");
                if (conditionList == null) {
                    conditionList = new ConditionList(subCondition);
                } else {
                    conditionList = new ConditionList(subCondition, conditionList, ConditionListRel.And);
                }
            }
            Condition condition = new Condition();
            condition.setTest(ConditionTest.NotEmpty);
            condition.setConditionList(conditionList);
            return condition;
        }
        return null;
    }

    private static @Nullable ConditionList processCriteria(Criteria criteria, Class<?> criteriaClass, TableInfo tableInfo, String prefix) {
        ConditionList conditionList = null;
        Set<String> set = new HashSet<>();
        for (Class<?> c = criteriaClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (set.contains(field.getName())) {
                    continue;
                }
                Criterion criterion = field.getAnnotation(Criterion.class);
                if (criterion != null) {
                    Condition condition = processCriterion(criterion, criteriaClass, tableInfo, field.getName(), field.getClass(), prefix);
                    if (conditionList == null) {
                        conditionList = new ConditionList(condition);
                    } else {
                        conditionList = new ConditionList(condition, conditionList, criteria.rel());
                    }
                    set.add(field.getName());
                }
            }
        }

        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(criteriaClass, Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException e) {
            throw new MybatisExtException(e);
        }

        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            if (set.contains(propertyDescriptor.getName())) {
                continue;
            }
            Method readMethod = propertyDescriptor.getReadMethod();
            if (readMethod == null) {
                continue;
            }
            Criterion criterion = readMethod.getAnnotation(Criterion.class);
            if (criterion != null) {
                Condition condition = processCriterion(criterion, criteriaClass, tableInfo, readMethod.getName(), readMethod.getReturnType(), prefix);
                if (conditionList == null) {
                    conditionList = new ConditionList(condition);
                } else {
                    conditionList = new ConditionList(condition, conditionList, criteria.rel());
                }
                set.add(readMethod.getName());
            }
        }

        return conditionList;
    }

    private static Condition processCriterion(Criterion criterion, Class<?> criteriaClass, TableInfo tableInfo, String variableName, Class<?> variableClass, String prefix) {
        Condition condition = new Condition();
        condition.setTest(criterion.test());
        Criteria criteria = variableClass.getAnnotation(Criteria.class);
        if (criteria != null) {
            ConditionList conditionList = processCriteria(criteria, variableClass, tableInfo, prefix + variableName + ".");
            condition.setConditionList(conditionList);
            return condition;
        }
        String property = StringUtils.isNotBlank(criterion.property()) ? criterion.property() : variableName;
        PropertyInfo propertyInfo = getPropertyInfo(tableInfo, property);
        condition.setPropertyInfo(propertyInfo);
        condition.setIgnorecase(criterion.ignorecase());
        condition.setNot(criterion.not());
        condition.setRel(criterion.rel());
        condition.setVariable(prefix + variableName);
        if (criterion.rel() == ConditionRel.Between) {
            checkHasProperty(criteriaClass, criterion.secondVariable());
            condition.setSecondVariable(prefix + criterion.secondVariable());
        }
        return condition;
    }

    private static PropertyInfo getPropertyInfo(TableInfo tableInfo, String property) {
        PropertyInfo propertyInfo = null;
        for (String s : property.split("\\.")) {
            if (propertyInfo == null) {
                propertyInfo = tableInfo.getNameToPropertyInfo().get(s);
            } else {
                propertyInfo = propertyInfo.getSubPropertyInfos().stream().filter(v -> v.getName().equals(s)).findFirst().orElse(null);
            }
            if (propertyInfo == null) {
                throw new MybatisExtException("Property '" + property + "' not found in TableInfo " + tableInfo.getTableClass().getName());
            }
        }
        return propertyInfo;
    }

    private static void checkHasProperty(Class<?> exampleClass, String property) {
        for (Class<?> c = exampleClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getName().equals(property)) {
                    return;
                }
            }
        }
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(exampleClass, Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException e) {
            throw new MybatisExtException(e);
        }
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            Method readMethod = propertyDescriptor.getReadMethod();
            if (readMethod == null && propertyDescriptor.getName().equals(property)) {
                return;
            }
        }
        throw new MybatisExtException("Property '" + property + "' not found in class " + exampleClass.getName());
    }
}
