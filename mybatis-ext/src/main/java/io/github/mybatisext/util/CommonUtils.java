package io.github.mybatisext.util;

import java.util.Collection;
import java.util.Optional;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import io.github.mybatisext.reflect.GenericType;

public class CommonUtils {

    public static boolean isSpecialParameter(Class<?> clazz) {
        return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
    }

    public static GenericType unwrapType(GenericType type) {
        if (type.isArray()) {
            return type.getComponentType();
        }
        if (Collection.class.isAssignableFrom(type.getType())) {
            return TypeArgumentResolver.resolveGenericTypeArgument(type, Collection.class, 0);
        }
        if (Optional.class.isAssignableFrom(type.getType())) {
            return TypeArgumentResolver.resolveGenericTypeArgument(type, Optional.class, 0);
        }
        return type;
    }
}
