package io.github.mybatisext.condition;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.Criteria;
import io.github.mybatisext.annotation.Criterion;
import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericField;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.resultmap.ResultType;
import io.github.mybatisext.util.StringUtils;

public class ConditionFactory {

    private static final Map<Class<?>, Map<String, Condition>> fromCriteriaCache = new ConcurrentHashMap<>();

    public static Condition fromCriteria(Configuration configuration, Class<?> exampleClass, String param) {
        Map<String, Condition> map = fromCriteriaCache.computeIfAbsent(exampleClass, k -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(param, k -> {
            Criteria criteria = exampleClass.getAnnotation(Criteria.class);
            TableInfo tableInfo = TableInfoFactory.getTableInfo(configuration, criteria.table() != void.class ? criteria.table() : exampleClass);
            ConditionComp conditionComp = processCriteria(criteria, GenericTypeFactory.build(exampleClass), tableInfo, param);
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
            if (conditionList.getRel() == ConditionCompRel.OR) {
                ConditionComp conditionComp = new ConditionComp(ConditionCompRel.AND);
                conditionComp.getConditions().addAll(andConditions);
                andConditions.clear();
                orConditions.add(conditionComp);
            }
        }
        if (!andConditions.isEmpty()) {
            ConditionComp conditionComp = new ConditionComp(ConditionCompRel.AND);
            conditionComp.getConditions().addAll(andConditions);
            orConditions.add(conditionComp);
        }
        ConditionComp conditionComp = new ConditionComp(ConditionCompRel.OR);
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
        ConditionComp conditionComp = new ConditionComp(ConditionCompRel.AND);
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            Condition condition = processPropertyInfo(propertyInfo, onlyById, test, param);
            if (condition == null) {
                continue;
            }
            conditionComp.getConditions().add(condition);
        }
        return conditionComp;
    }

    private static @Nullable Condition processPropertyInfo(PropertyInfo propertyInfo, boolean onlyById, IfTest test, String prefix) {
        if (onlyById && (propertyInfo.getResultType() == ResultType.RESULT || !propertyInfo.isOwnColumn())) {
            return null;
        }
        if (propertyInfo.getResultType() == ResultType.ID || propertyInfo.getResultType() == ResultType.RESULT) {
            ConditionTerm conditionTerm = new ConditionTerm();
            conditionTerm.setPropertyInfo(propertyInfo);
            conditionTerm.setRel(ConditionRel.Equals);
            conditionTerm.setVariable(new Variable(prefix, propertyInfo.getName(), propertyInfo.getJavaType()));
            conditionTerm.setTest(test);
            return conditionTerm;
        }
        if (propertyInfo.getResultType() == ResultType.ASSOCIATION) {
            ConditionComp conditionComp = new ConditionComp(ConditionCompRel.AND);
            conditionComp.setTest(test);
            conditionComp.setVariable(new Variable(prefix, propertyInfo.getName(), propertyInfo.getJavaType()));
            for (PropertyInfo subPropertyInfo : propertyInfo.values()) {
                Condition condition = processPropertyInfo(subPropertyInfo, onlyById, test, StringUtils.isNotBlank(prefix) ? prefix + "." + propertyInfo.getName() : propertyInfo.getName());
                conditionComp.getConditions().add(condition);
            }
            return conditionComp;
        }
        if (propertyInfo.getResultType() == ResultType.COLLECTION) {
            ConditionComp conditionComp = new ConditionComp(ConditionCompRel.AND);
            conditionComp.setTest(IfTest.NotEmpty);
            conditionComp.setVariable(new Variable(prefix, propertyInfo.getName(), propertyInfo.getJavaType()));
            for (PropertyInfo subPropertyInfo : propertyInfo.values()) {
                Condition condition = processPropertyInfo(subPropertyInfo, onlyById, test, (StringUtils.isNotBlank(prefix) ? prefix + "." + propertyInfo.getName() : propertyInfo.getName()) + "[0]");
                conditionComp.getConditions().add(condition);
            }
            return conditionComp;
        }
        return null;
    }

    private static ConditionComp processCriteria(Criteria criteria, GenericType genericType, TableInfo tableInfo, String prefix) {
        ConditionComp conditionComp = new ConditionComp(ConditionCompRel.AND);
        conditionComp.setVariable(new Variable(prefix, genericType));
        Set<String> set = new HashSet<>();
        for (GenericType c = genericType; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            for (GenericField field : c.getDeclaredFields()) {
                if (set.contains(field.getName())) {
                    continue;
                }
                Criterion criterion = field.getAnnotation(Criterion.class);
                if (criterion != null) {
                    Condition condition = processCriterion(criterion, genericType, tableInfo, field.getName(), field.getGenericType(), prefix);
                    conditionComp.getConditions().add(condition);
                    set.add(field.getName());
                }
            }
        }

        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(genericType.getType(), Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException e) {
            throw new MybatisExtException(e);
        }

        Map<Method, GenericMethod> methodMap = Arrays.stream(genericType.getMethods()).collect(Collectors.toMap(GenericMethod::getMethod, v -> v));
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            if (set.contains(propertyDescriptor.getName())) {
                continue;
            }
            GenericMethod readMethod = methodMap.get(propertyDescriptor.getReadMethod());
            if (readMethod == null) {
                continue;
            }
            Criterion criterion = readMethod.getAnnotation(Criterion.class);
            if (criterion != null) {
                Condition condition = processCriterion(criterion, genericType, tableInfo, propertyDescriptor.getName(), readMethod.getGenericReturnType(), prefix);
                conditionComp.getConditions().add(condition);
                set.add(propertyDescriptor.getName());
            }
        }

        return conditionComp;
    }

    private static Condition processCriterion(Criterion criterion, GenericType criteriaClass, TableInfo tableInfo, String variableName, GenericType variableClass, String prefix) {
        Criteria criteria = variableClass.getAnnotation(Criteria.class);
        if (criteria != null) {
            ConditionComp conditionComp = processCriteria(criteria, variableClass, tableInfo, StringUtils.isNotBlank(prefix) ? prefix + "." + variableName : variableName);
            conditionComp.setTest(criterion.test());
            conditionComp.setVariable(new Variable(prefix, variableName, variableClass));
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
        conditionTerm.setVariable(new Variable(prefix, variableName, propertyInfo.getJavaType()));
        if (criterion.rel() == ConditionRel.Between) {
            GenericType propertyGenericType = getPropertyGenericType(criteriaClass, criterion.secondVariable());
            conditionTerm.setSecondVariable(new Variable(prefix, variableName, propertyGenericType));
        }
        return conditionTerm;
    }

    private static PropertyInfo getPropertyInfo(TableInfo tableInfo, String property) {
        PropertyInfo propertyInfo = null;
        for (String s : property.split("\\.")) {
            if (propertyInfo == null) {
                propertyInfo = tableInfo.getNameToPropertyInfo().get(s);
            } else {
                propertyInfo = propertyInfo.values().stream().filter(v -> v.getName().equals(s)).findFirst().orElse(null);
            }
            if (propertyInfo == null) {
                throw new MybatisExtException("Property '" + property + "' not found in TableInfo " + tableInfo.getTableClass().getName());
            }
        }
        return propertyInfo;
    }

    private static GenericType getPropertyGenericType(GenericType exampleClass, String property) {
        for (GenericType c = exampleClass; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            for (GenericField field : c.getDeclaredFields()) {
                if (field.getName().equals(property)) {
                    return field.getGenericType();
                }
            }
        }
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(exampleClass.getType(), Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException e) {
            throw new MybatisExtException(e);
        }
        Map<Method, GenericMethod> methodMap = Arrays.stream(exampleClass.getMethods()).collect(Collectors.toMap(GenericMethod::getMethod, v -> v));
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            GenericMethod readMethod = methodMap.get(propertyDescriptor.getReadMethod());
            if (readMethod != null && propertyDescriptor.getName().equals(property)) {
                return readMethod.getGenericReturnType();
            }
        }
        throw new MybatisExtException("Property '" + property + "' not found in class " + exampleClass.getName());
    }
}
