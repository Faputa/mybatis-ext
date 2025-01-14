package io.github.mybatisext.util;

import java.lang.reflect.Type;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class TypeArgumentResolver {

    public static Class<?> resolveType(Type type, Class<?> sourceClass, int index) {
        GenericType genericType = resolveGenericType(type, sourceClass, index);
        if (genericType == null) {
            return null;
        }
        return genericType.getType();
    }

    public static GenericType resolveGenericType(Type type, Class<?> sourceClass, int index) {
        if (type == null) {
            return null;
        }
        GenericType genericType = type instanceof GenericType ? (GenericType) type : GenericTypeFactory.build(type);
        if (genericType.getType() == sourceClass) {
            return genericType.getTypeParameters()[index];
        }
        for (GenericType interfaceType : genericType.getGenericInterfaces()) {
            GenericType resolvedClass = resolveGenericType(interfaceType, sourceClass, index);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        return resolveGenericType(genericType.getGenericSuperclass(), sourceClass, index);
    }
}
