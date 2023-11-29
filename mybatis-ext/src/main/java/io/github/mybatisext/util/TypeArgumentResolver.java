package io.github.mybatisext.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * 解析泛型类型参数的实用工具类。
 */
public class TypeArgumentResolver {

    /**
     * 解析泛型类在指定索引处的类型参数。
     *
     * @param sourceType 起始解析的原始类。
     * @param targetType 目标泛型类。
     * @param index      要解析的类型参数的索引。
     * @return 解析得到的类型参数的Class对象，如果未找到则为null。
     */
    public static Class<?> resolveTypeArgument(Type sourceType, Class<?> targetType, int index) {
        return analyzeGenericType(sourceType, null, null, targetType, index);
    }

    private static Class<?> analyzeGenericType(Type genericType, @Nullable Class<?> childType, @Nullable Type[] childTypeArguments, Class<?> targetType, int index) {
        if (genericType instanceof ParameterizedType) {
            return analyzeParameterizedType((ParameterizedType) genericType, childType, childTypeArguments, targetType, index);
        }
        if (genericType instanceof Class) {
            return analyzeClass((Class<?>) genericType, null, targetType, index);
        }
        return null;
    }

    private static Class<?> analyzeParameterizedType(ParameterizedType parameterizedType, @Nullable Class<?> childType, @Nullable Type[] childTypeArguments, Class<?> targetType, int index) {
        Type rawType = parameterizedType.getRawType();
        if (rawType instanceof Class) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (childTypeArguments != null) {
                assert childType != null;
                adjustTypeArguments(childType, childTypeArguments, typeArguments);
            }
            return analyzeClass((Class<?>) rawType, typeArguments, targetType, index);
        }
        return null;
    }

    private static Class<?> analyzeClass(Class<?> classType, @Nullable Type[] classTypeArguments, Class<?> targetType, int index) {
        // 找到了
        if (classType == targetType && classTypeArguments != null && classTypeArguments[index] instanceof Class) {
            return (Class<?>) classTypeArguments[index];
        }
        // 检查接口
        for (Type interfaceType : classType.getGenericInterfaces()) {
            Class<?> resolvedClass = analyzeGenericType(interfaceType, classType, classTypeArguments, targetType, index);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        // 检查超类
        return analyzeGenericType(classType.getGenericSuperclass(), classType, classTypeArguments, targetType, index);
    }

    private static void adjustTypeArguments(Class<?> childType, Type[] childTypeArguments, Type[] typeArguments) {
        for (int i = 0; i < typeArguments.length; i++) {
            if (typeArguments[i] instanceof TypeVariable) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) typeArguments[i];
                TypeVariable<?>[] typeParameters = childType.getTypeParameters();
                for (int j = 0; j < typeParameters.length; j++) {
                    if (Objects.equals(typeVariable.getName(), typeParameters[j].getName())) {
                        typeArguments[i] = childTypeArguments[j];
                        break;
                    }
                }
            }
        }
    }
}
