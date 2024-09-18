package io.github.mybatisext.util;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

public class MybatisUtils {

    public static boolean isSpecialParameter(Class<?> clazz) {
        return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
    }
}
