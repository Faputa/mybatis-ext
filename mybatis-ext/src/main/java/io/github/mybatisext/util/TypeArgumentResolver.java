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
    public static Class<?> resolveTypeArgument(Class<?> sourceType, Class<?> targetType, int index) {
        return resolveTypeArgument(sourceType, null, targetType, index);
    }

    private static Class<?> resolveTypeArgument(Class<?> sourceType, @Nullable Type[] sourceTypeArguments, Class<?> targetType, int index) {
        // 找到了
        if (sourceType == targetType && sourceTypeArguments != null && sourceTypeArguments[index] instanceof Class) {
            return (Class<?>) sourceTypeArguments[index];
        }
        // 检查接口
        for (Type interfaceType : sourceType.getGenericInterfaces()) {
            Class<?> resolvedClass = analyzeGenericType(interfaceType, sourceType, sourceTypeArguments, targetType, index);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        // 检查超类
        return analyzeGenericType(sourceType.getGenericSuperclass(), sourceType, sourceTypeArguments, targetType, index);
    }

    private static Class<?> analyzeGenericType(Type genericType, Class<?> sourceType, @Nullable Type[] sourceTypeArguments, Class<?> targetType, int index) {
        if (genericType instanceof ParameterizedType) {
            return analyzeParameterizedType((ParameterizedType) genericType, sourceType, sourceTypeArguments, targetType, index);
        }
        if (genericType instanceof Class) {
            return resolveTypeArgument((Class<?>) genericType, targetType, index);
        }
        return null;
    }

    private static Class<?> analyzeParameterizedType(ParameterizedType parameterizedType, Class<?> sourceType, @Nullable Type[] sourceTypeArguments, Class<?> targetType, int index) {
        Type rawType = parameterizedType.getRawType();
        if (rawType instanceof Class) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (sourceTypeArguments != null) {
                adjustTypeArguments(sourceType, sourceTypeArguments, typeArguments);
            }
            return resolveTypeArgument((Class<?>) rawType, typeArguments, targetType, index);
        }
        return null;
    }

    private static void adjustTypeArguments(Class<?> sourceType, Type[] sourceTypeArguments, Type[] typeArguments) {
        for (int i = 0; i < typeArguments.length; i++) {
            if (typeArguments[i] instanceof TypeVariable) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) typeArguments[i];
                TypeVariable<?>[] typeParameters = sourceType.getTypeParameters();
                for (int j = 0; j < typeParameters.length; j++) {
                    if (Objects.equals(typeVariable.getName(), typeParameters[j].getName())) {
                        typeArguments[i] = sourceTypeArguments[j];
                        break;
                    }
                }
            }
        }
    }
}
