package io.github.mybatisext.reflect;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

public class GenericTypeFactory {

    public static GenericType build(Type type) {
        return build(type, null, null);
    }

    public static GenericType build(Type type, Class<?> actualClass, Type[] actualTypeArguments) {
        if (type instanceof Class) {
            return new GenericType((Class<?>) type, actualTypeArguments);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments != null) {
                    adjustTypeArguments(typeArguments, actualClass, actualTypeArguments);
                }
                return new GenericType((Class<?>) rawType, typeArguments);
            }
        } else if (type instanceof TypeVariable) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) type;
            TypeVariable<?>[] typeParameters = actualClass.getTypeParameters();
            for (int j = 0; j < typeParameters.length; j++) {
                if (actualTypeArguments[j] instanceof Class && Objects.equals(typeVariable.getName(), typeParameters[j].getName())) {
                    return new GenericType((Class<?>) actualTypeArguments[j], null);
                }
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            return build(genericArrayType.getGenericComponentType(), actualClass, actualTypeArguments);
        }
        return null;
    }

    private static void adjustTypeArguments(Type[] typeArguments, Class<?> actualClass, Type[] actualTypeArguments) {
        for (int i = 0; i < typeArguments.length; i++) {
            if (typeArguments[i] instanceof TypeVariable) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) typeArguments[i];
                TypeVariable<?>[] typeParameters = actualClass.getTypeParameters();
                for (int j = 0; j < typeParameters.length; j++) {
                    if (Objects.equals(typeVariable.getName(), typeParameters[j].getName())) {
                        typeArguments[i] = actualTypeArguments[j];
                        break;
                    }
                }
            }
        }
    }
}
