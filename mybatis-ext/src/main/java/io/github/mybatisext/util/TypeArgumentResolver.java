package io.github.mybatisext.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

public class TypeArgumentResolver {

    public static Class<?> resolveTypeArgument(Type currentType, Class<?> sourceClass, int index) {
        return resolveTypeArgument(currentType, sourceClass, index, null, null);
    }

    private static Class<?> resolveTypeArgument(Type currentType, Class<?> sourceClass, int index, Class<?> actualClass, Type[] actualTypeArguments) {
        Class<?> currentClass;
        Type[] currentTypeArguments;
        if (currentType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) currentType;
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
        } else if (currentType instanceof Class) {
            currentClass = (Class<?>) currentType;
            currentTypeArguments = actualTypeArguments;
        } else {
            return null;
        }
        if (currentClass == sourceClass) {
            if (currentTypeArguments != null && currentTypeArguments[index] instanceof Class) {
                return (Class<?>) currentTypeArguments[index];
            }
            return null;
        }
        for (Type interfaceType : currentClass.getGenericInterfaces()) {
            Class<?> resolvedClass = resolveTypeArgument(interfaceType, sourceClass, index, currentClass, currentTypeArguments);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        return resolveTypeArgument(currentClass.getGenericSuperclass(), sourceClass, index, currentClass, currentTypeArguments);
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
