package io.github.mybatisext.util;

import java.lang.reflect.Type;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class TypeArgumentResolver {

    public static Class<?> resolveType(Type sourceType, Class<?> targetType, int index) {
        GenericType genericType = resolveGenericType(sourceType, targetType, index);
        if (genericType == null) {
            return null;
        }
        return genericType.getType();
    }

    public static GenericType resolveGenericType(Type sourceType, Class<?> targetType, int index) {
        if (sourceType == null) {
            return null;
        }
        GenericType genericType = sourceType instanceof GenericType ? (GenericType) sourceType : GenericTypeFactory.build(sourceType);
        if (genericType.getType() == targetType) {
            return genericType.getTypeParameters()[index];
        }
        for (GenericType interfaceType : genericType.getGenericInterfaces()) {
            GenericType resolvedClass = resolveGenericType(interfaceType, targetType, index);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        return resolveGenericType(genericType.getGenericSuperclass(), targetType, index);
    }
}
