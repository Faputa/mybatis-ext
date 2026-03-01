package io.github.mybatisext.util;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class TypeUtils {

    public static boolean isSpecialParameter(Class<?> clazz) {
        return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
    }

    public static Class<?> unwrapToClass(GenericType type) {
        if (type.isArray()) {
            return type.getComponentType().getType();
        }
        if (Collection.class.isAssignableFrom(type.getType())) {
            return resolveTypeArgument(type, Collection.class, 0);
        }
        if (Optional.class.isAssignableFrom(type.getType())) {
            return resolveTypeArgument(type, Optional.class, 0);
        }
        return type.getType();
    }

    public static GenericType unwrapToGenericType(GenericType type) {
        if (type.isArray()) {
            return type.getComponentType();
        }
        if (Collection.class.isAssignableFrom(type.getType())) {
            return resolveGenericTypeArgument(type, Collection.class, 0);
        }
        if (Optional.class.isAssignableFrom(type.getType())) {
            return resolveGenericTypeArgument(type, Optional.class, 0);
        }
        return type;
    }

    public static Class<?> resolveTypeArgument(Type type, Class<?> genericClass, int index) {
        GenericType genericType = resolveGenericTypeArgument(type, genericClass, index);
        if (genericType == null) {
            return null;
        }
        return genericType.getType();
    }

    public static GenericType resolveGenericTypeArgument(Type type, Class<?> genericClass, int index) {
        if (type == null) {
            return null;
        }
        GenericType genericType = type instanceof GenericType ? (GenericType) type : GenericTypeFactory.build(type);
        if (genericType.getType() == genericClass) {
            return genericType.getTypeParameters()[index];
        }
        for (GenericType interfaceType : genericType.getGenericInterfaces()) {
            GenericType resolvedClass = resolveGenericTypeArgument(interfaceType, genericClass, index);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        return resolveGenericTypeArgument(genericType.getGenericSuperclass(), genericClass, index);
    }
}
