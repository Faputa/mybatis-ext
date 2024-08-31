package io.github.mybatisext.util;

import java.lang.reflect.Type;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class TypeArgumentResolver {

    public static Class<?> resolveTypeArgument(Type sourceType, Class<?> targetType, int index) {
        if (sourceType == null) {
            return null;
        }
        if (!(sourceType instanceof GenericType)) {
            sourceType = GenericTypeFactory.build(sourceType);
        }
        GenericType genericType = (GenericType) sourceType;
        if (genericType.getType() == targetType) {
            return genericType.getTypeParameters()[index].getType();
        }
        for (GenericType interfaceType : genericType.getGenericInterfaces()) {
            Class<?> resolvedClass = resolveTypeArgument(interfaceType, targetType, index);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        return resolveTypeArgument(genericType.getGenericSuperclass(), targetType, index);
    }
}
