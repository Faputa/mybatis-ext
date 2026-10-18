package io.github.mybatisext.util;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class TypeUtils {

    private static final Map<String, Class<?>> NAME_TO_PRIMITIVE_TYPE;
    private static final Map<String, Class<?>> NAME_TO_BOXED_TYPE;

    static {
        Map<String, Class<?>> nameToPrimitiveType = new HashMap<>();
        Map<String, Class<?>> nameToBoxedType = new HashMap<>();
        Class<?>[][] pairs = {
                {boolean.class, Boolean.class},
                {byte.class, Byte.class},
                {short.class, Short.class},
                {int.class, Integer.class},
                {long.class, Long.class},
                {float.class, Float.class},
                {double.class, Double.class},
                {char.class, Character.class}
        };
        for (Class<?>[] pair : pairs) {
            nameToPrimitiveType.put(pair[0].getName(), pair[0]);
            nameToBoxedType.put(pair[0].getName(), pair[1]);
            nameToBoxedType.put(pair[1].getName(), pair[1]);
        }
        NAME_TO_PRIMITIVE_TYPE = Collections.unmodifiableMap(nameToPrimitiveType);
        NAME_TO_BOXED_TYPE = Collections.unmodifiableMap(nameToBoxedType);
    }

    public static Class<?> resolveClass(String name) throws ClassNotFoundException {
        Class<?> primitive = NAME_TO_PRIMITIVE_TYPE.get(name);
        return primitive != null ? primitive : Class.forName(name);
    }

    public static Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        return NAME_TO_BOXED_TYPE.getOrDefault(type.getName(), type);
    }

    /** 仅忽略基本类型与对应包装类型的差异，其他类型保留自身的相等性规则。 */
    public static boolean equalsIgnoreBoxing(JavaType first, JavaType second) {
        Class<?> firstBoxed = findBoxedPrimitiveType(first);
        Class<?> secondBoxed = findBoxedPrimitiveType(second);
        if (firstBoxed != null || secondBoxed != null) {
            return firstBoxed == secondBoxed;
        }
        return Objects.equals(first, second);
    }

    /** 与 equalsIgnoreBoxing 使用相同的装箱等价规则。 */
    public static int hashCodeIgnoreBoxing(JavaType type) {
        Class<?> boxed = findBoxedPrimitiveType(type);
        return boxed == null ? Objects.hashCode(type) : boxed.hashCode();
    }

    private static Class<?> findBoxedPrimitiveType(JavaType type) {
        return type != null && type.isResolved() ? NAME_TO_BOXED_TYPE.get(type.getName()) : null;
    }

    public static boolean isSpecialParameter(Class<?> clazz) {
        return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
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
