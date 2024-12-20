package io.github.mybatisext.jpa;

import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.reflect.GenericField;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import org.apache.ibatis.session.Configuration;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class VariableFactory {

    public static Variable build(Configuration configuration, String name, GenericType javaType) {
        return build(configuration, "", name, javaType);
    }

    public static Variable build(Configuration configuration, String prefix, String name, GenericType javaType) {
        Variable variable = new Variable(prefix, name, javaType);
        if (!hasSubVariable(configuration, javaType.getType())) {
            return variable;
        }
        for (GenericType c = javaType; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            for (GenericField field : c.getDeclaredFields()) {
                if (variable.containsKey(field.getName())) {
                    continue;
                }
                variable.put(field.getName(), build(configuration, variable.getFullName(), field.getName(), field.getGenericType()));
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
            if (variable.containsKey(propertyDescriptor.getName())) {
                continue;
            }
            GenericMethod readMethod = methodMap.get(propertyDescriptor.getReadMethod());
            if (readMethod == null) {
                continue;
            }
            if (readMethod.getMethod().getDeclaringClass() == Object.class) {
                continue;
            }
            variable.put(propertyDescriptor.getName(), build(configuration, variable.getFullName(), propertyDescriptor.getName(), readMethod.getGenericReturnType()));
        }
        return variable;
    }

    public static boolean hasSubVariable(Configuration configuration, Class<?> javaType) {
        return !configuration.getTypeHandlerRegistry().hasTypeHandler(javaType) && !Map.class.isAssignableFrom(javaType) && !Collection.class.isAssignableFrom(javaType) && !javaType.isArray();
    }
}
