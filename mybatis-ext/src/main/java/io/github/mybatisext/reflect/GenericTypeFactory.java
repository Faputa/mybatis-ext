package io.github.mybatisext.reflect;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

public class GenericTypeFactory {

    public static GenericType build(Type type) {
        return build(type, new HashMap<>());
    }

    public static GenericType build(Type type, Map<TypeVariable<?>, Type> typeMap) {
        if (type instanceof Class) {
            return new GenericType((Class<?>) type, typeMap);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class) {
                mapTypeVariables(typeMap, (Class<?>) rawType, parameterizedType.getActualTypeArguments());
                return new GenericType((Class<?>) rawType, typeMap);
            }
        } else if (type instanceof TypeVariable) {
            return build(typeMap.get(type), typeMap);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            return build(genericArrayType.getGenericComponentType(), typeMap);
        }
        return null;
    }

    private static void mapTypeVariables(Map<TypeVariable<?>, Type> typeMap, Class<?> rawType, Type[] actualTypeArguments) {
        TypeVariable<?>[] typeVariables = rawType.getTypeParameters();
        for (int i = 0; i < typeVariables.length; i++) {
            typeMap.put(typeVariables[i], actualTypeArguments[i]);
        }
    }
}
