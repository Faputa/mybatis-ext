package io.github.mybatisext.util;

import java.lang.reflect.Type;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class TypeArgumentResolver {

    public static Class<?>  resolveTypeArgument(Type type, Class<?> sourceClass, int index) {
        GenericType genericType = resolveGenericTypeArgument(type, sourceClass, index);
        if (genericType == null) {
            return null;
        }
        return genericType.getType();
    }

    public static GenericType resolveGenericTypeArgument(Type type, Class<?> sourceClass, int index) {
        if (type == null) {
            return null;
        }
        GenericType genericType = type instanceof GenericType ? (GenericType) type : GenericTypeFactory.build(type);
        if (genericType.getType() == sourceClass) {
            return genericType.getTypeParameters()[index];
        }
        for (GenericType interfaceType : genericType.getGenericInterfaces()) {
            GenericType resolvedClass = resolveGenericTypeArgument(interfaceType, sourceClass, index);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        return resolveGenericTypeArgument(genericType.getGenericSuperclass(), sourceClass, index);
    }
}
