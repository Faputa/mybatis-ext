package io.github.mybatisext.jpa;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.util.StringUtils;

public class Variable {

    private final String name;
    private final String fullName;
    private final Class<?> javaType;
    private List<Variable> subVariables;

    public Variable(String name, Class<?> javaType) {
        this("", name, javaType);
    }

    public Variable(String prefix, String name, Class<?> javaType) {
        this.name = name;
        this.fullName = StringUtils.isNotBlank(prefix) ? prefix + "." + name : name;
        this.javaType = javaType;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public static boolean hasSubVariable(Configuration configuration, Class<?> javaType) {
        return !configuration.getTypeHandlerRegistry().hasTypeHandler(javaType) && !Map.class.isAssignableFrom(javaType) && !Collection.class.isAssignableFrom(javaType) && !javaType.isArray();
    }

    public List<Variable> getSubVariable(Configuration configuration) {
        if (subVariables != null) {
            return subVariables;
        }
        subVariables = new ArrayList<>();
        if (!hasSubVariable(configuration, javaType)) {
            return subVariables;
        }
        Set<String> set = new HashSet<>();
        for (Class<?> c = javaType; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (set.contains(field.getName())) {
                    continue;
                }
                subVariables.add(new Variable(fullName, field.getName(), field.getType()));
                set.add(field.getName());
            }
        }
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(javaType, Introspector.IGNORE_ALL_BEANINFO);
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
            subVariables.add(new Variable(fullName, readMethod.getName(), readMethod.getReturnType()));
            set.add(readMethod.getName());
        }
        return subVariables;
    }
}
