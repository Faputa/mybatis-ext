package io.github.mybatisext.util;

import java.lang.reflect.Type;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class TypeArgumentResolver {

    public static Class<?> resolveTypeArgument(Type sourceType, Class<?> targetType, int index) {
        GenericType genericType = resolveGenericTypeArgument(sourceType, targetType, index);
        if (genericType == null) {
            return null;
        }
        return genericType.getType();
    }

    public static GenericType resolveGenericTypeArgument(Type sourceType, Class<?> targetType, int index) {
        if (sourceType == null) {
            return null;
        }
        GenericType genericType = sourceType instanceof GenericType ? (GenericType) sourceType : GenericTypeFactory.build(sourceType);
        if (genericType.getType() == targetType) {
            return genericType.getTypeParameters()[index];
        }
        for (GenericType interfaceType : genericType.getGenericInterfaces()) {
            GenericType resolvedClass = resolveGenericTypeArgument(interfaceType, targetType, index);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        return resolveGenericTypeArgument(genericType.getGenericSuperclass(), targetType, index);
    }
}
