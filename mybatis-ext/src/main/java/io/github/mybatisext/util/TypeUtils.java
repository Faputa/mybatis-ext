package io.github.mybatisext.util;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class TypeUtils {

    public static Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    public static Class<?> resolveClass(String name) throws ClassNotFoundException {
        if ("boolean".equals(name)) {
            return boolean.class;
        }
        if ("byte".equals(name)) {
            return byte.class;
        }
        if ("short".equals(name)) {
            return short.class;
        }
        if ("int".equals(name)) {
            return int.class;
        }
        if ("long".equals(name)) {
            return long.class;
        }
        if ("float".equals(name)) {
            return float.class;
        }
        if ("double".equals(name)) {
            return double.class;
        }
        if ("char".equals(name)) {
            return char.class;
        }
        return Class.forName(name);
    }

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
