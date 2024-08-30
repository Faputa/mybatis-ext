package io.github.mybatisext.util;

import java.lang.reflect.Type;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class TypeArgumentResolver {

    public static Class<?> resolveTypeArgument(Type type, Class<?> sourceClass, int index) {
        if (type == null) {
            return null;
        }
        if (!(type instanceof GenericType)) {
            type = GenericTypeFactory.build(type);
        }
        GenericType genericType = (GenericType) type;
        if (genericType.getType() == sourceClass) {
            if (genericType.getTypeParameters() != null && genericType.getTypeParameters()[index] instanceof Class) {
                return (Class<?>) genericType.getTypeParameters()[index];
            }
            return null;
        }
        for (GenericType interfaceType : genericType.getGenericInterfaces()) {
            Class<?> resolvedClass = resolveTypeArgument(interfaceType, sourceClass, index);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        return resolveTypeArgument(genericType.getGenericSuperclass(), sourceClass, index);
    }
}
