package com.mybatisext.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

import javax.annotation.Nullable;

public class TypeArgumentResolver {

    public static Class<?> findClass(Class<?> clazz, Class<?> search, int index, @Nullable Class<?> fixedClass) {
        return findClass(clazz, search, index, fixedClass, null);
    }

    private static Class<?> findClass(Class<?> clazz, Class<?> search, int index, @Nullable Class<?> fixedClass,
            @Nullable Type[] clazzTypeArguments) {
        // 检查固定类是否在继承路径上
        if (fixedClass != null && !clazz.isAssignableFrom(fixedClass) && !fixedClass.isAssignableFrom(clazz)) {
            return null;
        }
        // 找到了
        if (clazz == search) {
            if (clazzTypeArguments != null && clazzTypeArguments[index] instanceof Class) {
                return (Class<?>) clazzTypeArguments[index];
            }
        }
        // 检查基接口
        Type[] genericInterfaces = clazz.getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
                // 泛型基接口
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    adjustTypeArguments(clazz, clazzTypeArguments, typeArguments);
                    Class<?> entityClass = findClass((Class<?>) rawType, search, index, fixedClass, typeArguments);
                    if (entityClass != null) {
                        return entityClass;
                    }
                }
            } else if (type instanceof Class) {
                // 其他基接口
                Class<?> entityClass = findClass((Class<?>) type, search, index, fixedClass);
                if (entityClass != null) {
                    return entityClass;
                }
            }
        }
        // 检查基类
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || superclass == Object.class) {
            return null;
        }
        Type[] typeArguments = superclass.getTypeParameters();
        adjustTypeArguments(clazz, clazzTypeArguments, typeArguments);
        return findClass(superclass, search, index, fixedClass, typeArguments);
    }

    private static void adjustTypeArguments(Class<?> clazz, @Nullable Type[] clazzTypeArguments, Type[] typeArguments) {
        if (clazzTypeArguments == null) {
            return;
        }
        for (int i = 0; i < typeArguments.length; i++) {
            if (typeArguments[i] instanceof TypeVariable) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) typeArguments[i];
                TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
                for (int j = 0; j < typeParameters.length; j++) {
                    if (Objects.equals(typeVariable.getName(), typeParameters[j].getName())) {
                        typeArguments[i] = clazzTypeArguments[j];
                        break;
                    }
                }
            }
        }
    }

}
