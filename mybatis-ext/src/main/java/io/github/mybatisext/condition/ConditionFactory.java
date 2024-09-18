package io.github.mybatisext.condition;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.Criteria;
import io.github.mybatisext.annotation.Criterion;
import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.resultmap.ResultType;
import io.github.mybatisext.util.StringUtils;

public class ConditionFactory {

    private static final Map<Class<?>, Map<String, Condition>> fromCriteriaCache = new ConcurrentHashMap<>();

    public static Condition fromCriteria(Configuration configuration, Class<?> exampleClass, String param) {
        Map<String, Condition> map = fromCriteriaCache.computeIfAbsent(exampleClass, k -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(param, k -> {
            Criteria criteria = exampleClass.getAnnotation(Criteria.class);
            TableInfo tableInfo = TableInfoFactory.getTableInfo(configuration, criteria.table() != void.class ? criteria.table() : exampleClass);
            ConditionComp conditionComp = processCriteria(criteria, exampleClass, tableInfo, StringUtils.isNotBlank(param) ? param + "." : "");
            return simplifyCondition(conditionComp);
        });
    }

    private static final Map<TableInfo, Map<Boolean, Map<IfTest, Map<String, Condition>>>> fromTableInfoCache = new ConcurrentHashMap<>();

    public static Condition fromTableInfo(TableInfo tableInfo, boolean onlyById, IfTest test, String param) {
        Map<Boolean, Map<IfTest, Map<String, Condition>>> map = fromTableInfoCache.computeIfAbsent(tableInfo, k -> new ConcurrentHashMap<>());
        Map<IfTest, Map<String, Condition>> map2 = map.computeIfAbsent(onlyById, k -> new ConcurrentHashMap<>());
        Map<String, Condition> map3 = map2.computeIfAbsent(test, k -> new ConcurrentHashMap<>());
        return map3.computeIfAbsent(param, k -> {
            ConditionComp conditionComp = processTableInfo(tableInfo, onlyById, test, param);
            if (conditionComp.getConditions().isEmpty() && onlyById) {
                conditionComp = processTableInfo(tableInfo, false, test, param);
            }
            return simplifyCondition(conditionComp);
        });
    }

    public static Condition fromConditionList(ConditionList conditionList) {
        List<Condition> andConditions = new ArrayList<>();
        List<Condition> orConditions = new ArrayList<>();
        for (; conditionList != null; conditionList = conditionList.getTailList()) {
            andConditions.add(conditionList.getCondition());
            if (conditionList.getRel() == ConditionCompRel.Or) {
                ConditionComp conditionComp = new ConditionComp(ConditionCompRel.And);
                conditionComp.getConditions().addAll(andConditions);
                andConditions.clear();
                orConditions.add(conditionComp);
            }
        }
        if (!andConditions.isEmpty()) {
            ConditionComp conditionComp = new ConditionComp(ConditionCompRel.And);
            conditionComp.getConditions().addAll(andConditions);
            orConditions.add(conditionComp);
        }
        ConditionComp conditionComp = new ConditionComp(ConditionCompRel.Or);
        conditionComp.getConditions().addAll(orConditions);
        return simplifyCondition(conditionComp);
    }

    public static @Nullable Condition simplifyCondition(Condition condition) {
        if (condition instanceof ConditionTerm) {
            return condition;
        }
        if (condition instanceof ConditionComp) {
            ConditionComp conditionComp = (ConditionComp) condition;
            if (conditionComp.getConditions().size() == 1 && conditionComp.getTest() == IfTest.None) {
                return simplifyCondition(conditionComp.getConditions().iterator().next());
            }
            for (Condition c : new ArrayList<>(conditionComp.getConditions())) {
                conditionComp.getConditions().remove(c);
                Condition simplifyCondition = simplifyCondition(c);
                if (simplifyCondition == null) {
                    continue;
                }
                conditionComp.getConditions().add(simplifyCondition);
            }
            if (conditionComp.getConditions().isEmpty()) {
                return null;
            }
            return conditionComp;
        }
        return condition;
    }

    private static ConditionComp processTableInfo(TableInfo tableInfo, boolean onlyById, IfTest test, String param) {
        ConditionComp conditionComp = new ConditionComp(ConditionCompRel.And);
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            Condition condition = processPropertyInfo(propertyInfo, onlyById, test, StringUtils.isNotBlank(param) ? param + "." : "");
            if (condition == null) {
                continue;
            }
            conditionComp.getConditions().add(condition);
        }
        return conditionComp;
    }

    private static @Nullable Condition processPropertyInfo(PropertyInfo propertyInfo, boolean onlyById, IfTest test, String prefix) {
        if (onlyById && (propertyInfo.getResultType() == ResultType.RESULT || !propertyInfo.isOwn())) {
            return null;
        }
        if (propertyInfo.getResultType() == ResultType.ID || propertyInfo.getResultType() == ResultType.RESULT) {
            ConditionTerm conditionTerm = new ConditionTerm();
            conditionTerm.setPropertyInfo(propertyInfo);
            conditionTerm.setRel(ConditionRel.Equals);
            conditionTerm.setVariable(prefix + propertyInfo.getName());
            conditionTerm.setTest(test);
            return conditionTerm;
        }
        if (propertyInfo.getResultType() == ResultType.ASSOCIATION) {
            ConditionComp conditionComp = new ConditionComp(ConditionCompRel.And);
            conditionComp.setTest(test);
            for (PropertyInfo subPropertyInfo : propertyInfo.getSubPropertyInfos()) {
                Condition condition = processPropertyInfo(subPropertyInfo, onlyById, test, prefix + propertyInfo.getName() + ".");
                conditionComp.getConditions().add(condition);
            }
            return conditionComp;
        }
        if (propertyInfo.getResultType() == ResultType.COLLECTION) {
            ConditionComp conditionComp = new ConditionComp(ConditionCompRel.And);
            conditionComp.setTest(test);
            for (PropertyInfo subPropertyInfo : propertyInfo.getSubPropertyInfos()) {
                Condition condition = processPropertyInfo(subPropertyInfo, onlyById, test, prefix + propertyInfo.getName() + "[0].");
                conditionComp.getConditions().add(condition);
            }
            return conditionComp;
        }
        return null;
    }

    private static ConditionComp processCriteria(Criteria criteria, Class<?> criteriaClass, TableInfo tableInfo, String prefix) {
        ConditionComp conditionComp = new ConditionComp(ConditionCompRel.And);
        Set<String> set = new HashSet<>();
        for (Class<?> c = criteriaClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (set.contains(field.getName())) {
                    continue;
                }
                Criterion criterion = field.getAnnotation(Criterion.class);
                if (criterion != null) {
                    Condition condition = processCriterion(criterion, criteriaClass, tableInfo, field.getName(), field.getClass(), prefix);
                    conditionComp.getConditions().add(condition);
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
                conditionComp.getConditions().add(condition);
                set.add(readMethod.getName());
            }
        }

        return conditionComp;
    }

    private static Condition processCriterion(Criterion criterion, Class<?> criteriaClass, TableInfo tableInfo, String variableName, Class<?> variableClass, String prefix) {
        Criteria criteria = variableClass.getAnnotation(Criteria.class);
        if (criteria != null) {
            ConditionComp conditionComp = processCriteria(criteria, variableClass, tableInfo, prefix + variableName + ".");
            conditionComp.setTest(criterion.test());
            return conditionComp;
        }
        String property = StringUtils.isNotBlank(criterion.property()) ? criterion.property() : variableName;
        PropertyInfo propertyInfo = getPropertyInfo(tableInfo, property);
        ConditionTerm conditionTerm = new ConditionTerm();
        conditionTerm.setTest(criterion.test());
        conditionTerm.setPropertyInfo(propertyInfo);
        conditionTerm.setIgnorecase(criterion.ignorecase());
        conditionTerm.setNot(criterion.not());
        conditionTerm.setRel(criterion.rel());
        conditionTerm.setVariable(prefix + variableName);
        if (criterion.rel() == ConditionRel.Between) {
            checkHasProperty(criteriaClass, criterion.secondVariable());
            conditionTerm.setSecondVariable(prefix + criterion.secondVariable());
        }
        return conditionTerm;
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
