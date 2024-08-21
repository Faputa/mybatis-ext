package io.github.mybatisext.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

public class TypeArgumentResolver {

    public static Class<?> resolveTypeArgument(Type sourceType, Class<?> targetType, int index) {
        return resolveTypeArgument(sourceType, targetType, index, null, null);
    }

    private static Class<?> resolveTypeArgument(Type sourceType, Class<?> targetType, int index, Class<?> actualClass, Type[] actualTypeArguments) {
        Class<?> currentClass;
        Type[] currentTypeArguments;
        if (sourceType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) sourceType;
            Type rawType = parameterizedType.getRawType();
            if (!(rawType instanceof Class)) {
                return null;
            }
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments != null) {
                adjustTypeArguments(actualClass, actualTypeArguments, typeArguments);
            }
            currentClass = (Class<?>) rawType;
            currentTypeArguments = typeArguments;
        } else if (sourceType instanceof Class) {
            currentClass = (Class<?>) sourceType;
            currentTypeArguments = actualTypeArguments;
        } else {
            return null;
        }
        if (currentClass == targetType) {
            if (currentTypeArguments != null && currentTypeArguments[index] instanceof Class) {
                return (Class<?>) currentTypeArguments[index];
            }
            return null;
        }
        for (Type interfaceType : currentClass.getGenericInterfaces()) {
            Class<?> resolvedClass = resolveTypeArgument(interfaceType, targetType, index, currentClass, currentTypeArguments);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        return resolveTypeArgument(currentClass.getGenericSuperclass(), targetType, index, currentClass, currentTypeArguments);
    }

    private static void adjustTypeArguments(Class<?> actualClass, Type[] actualTypeArguments, Type[] typeArguments) {
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
