package io.github.mybatisext.jpa;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.reflect.GenericField;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;

public class VariableFactory {

    public static void addChildren(Configuration configuration, Variable variable) {
        Map<String, Variable> nameToVariable = variable.getNameToVariable();
        if (!nameToVariable.isEmpty()) {
            return;
        }
        GenericType javaType = variable.getJavaType();
        if (!hasSubVariable(configuration, javaType)) {
            return;
        }

        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(javaType.getType(), Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException e) {
            throw new MybatisExtException(e);
        }
        Map<Method, GenericMethod> methodMap = Arrays.stream(javaType.getMethods()).collect(Collectors.toMap(GenericMethod::getMethod, v -> v));

        for (GenericType c = javaType; c != null && c.getType() != Object.class; c = c.getGenericSuperclass()) {
            for (GenericField field : c.getDeclaredFields()) {
                if (nameToVariable.containsKey(field.getName())) {
                    continue;
                }
                nameToVariable.put(field.getName(), new Variable(variable.getFullName(), field.getName(), field.getGenericType()));
            }
            for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                if (nameToVariable.containsKey(propertyDescriptor.getName())) {
                    continue;
                }
                GenericMethod readMethod = methodMap.get(propertyDescriptor.getReadMethod());
                if (readMethod == null || readMethod.getMethod().getDeclaringClass() != c.getType()) {
                    continue;
                }
                nameToVariable.put(propertyDescriptor.getName(), new Variable(variable.getFullName(), propertyDescriptor.getName(), readMethod.getGenericReturnType()));
            }
        }
    }

    public static boolean hasSubVariable(Configuration configuration, GenericType genericType) {
        return hasSubVariable(configuration, genericType.getType());
    }

    public static boolean hasSubVariable(Configuration configuration, Class<?> javaType) {
        return !configuration.getTypeHandlerRegistry().hasTypeHandler(javaType) && !Map.class.isAssignableFrom(javaType) && !Collection.class.isAssignableFrom(javaType) && !javaType.isArray();
    }
}
