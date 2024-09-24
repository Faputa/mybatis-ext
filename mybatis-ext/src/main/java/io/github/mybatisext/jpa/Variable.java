package io.github.mybatisext.jpa;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.reflect.GenericField;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.util.StringUtils;

public class Variable {

    private final String name;
    private final String fullName;
    private final GenericType javaType;
    private List<Variable> subVariables;

    public Variable(String name, GenericType javaType) {
        this("", name, javaType);
    }

    public Variable(String prefix, String name, GenericType javaType) {
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

    public GenericType getJavaType() {
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
        if (!hasSubVariable(configuration, javaType.getType())) {
            return subVariables;
        }
        Set<String> set = new HashSet<>();
        for (GenericType c = javaType; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            for (GenericField field : c.getDeclaredFields()) {
                if (set.contains(field.getName())) {
                    continue;
                }
                subVariables.add(new Variable(fullName, field.getName(), field.getGenericType()));
                set.add(field.getName());
            }
        }
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(javaType.getType(), Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException e) {
            throw new MybatisExtException(e);
        }
        Map<Method, GenericMethod> methodMap = Arrays.stream(javaType.getMethods()).collect(Collectors.toMap(GenericMethod::getMethod, v -> v));
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            if (set.contains(propertyDescriptor.getName())) {
                continue;
            }
            GenericMethod readMethod = methodMap.get(propertyDescriptor.getReadMethod());
            if (readMethod == null) {
                continue;
            }
            subVariables.add(new Variable(fullName, readMethod.getName(), readMethod.getGenericReturnType()));
            set.add(readMethod.getName());
        }
        return subVariables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Variable variable = (Variable) o;
        return Objects.equals(name, variable.name) && Objects.equals(fullName, variable.fullName) && Objects.equals(javaType, variable.javaType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fullName, javaType);
    }

    @Override
    public String toString() {
        return fullName;
    }
}
