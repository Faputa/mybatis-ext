package io.github.mybatisext.analysis;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import io.github.mybatisext.annotation.MapTable;
import io.github.mybatisext.mapper.ExtMapper;
import io.github.mybatisext.model.JavaAnnotation;
import io.github.mybatisext.model.JavaMethod;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.JavaTypes;

public final class MapperResolver {

    private MapperResolver() {
    }

    public static boolean isEnhancedMapper(JavaType mapper) {
        return mapper.isInterface() && mapper.getTypeParameters().length == 0
                && (mapper.hasAnnotation(MapTable.class) || JavaTypes.isAssignableTo(mapper, ExtMapper.class.getName()));
    }

    public static JavaType entityType(JavaType mapper) {
        JavaAnnotation annotation = mapper.annotation(MapTable.class);
        return annotation != null
                ? annotation.value("value")
                : JavaTypes.typeArgument(mapper, ExtMapper.class.getName(), 0);
    }

    /** 适配器通过完整 ID 报告已有的 XML 或注解语句。 */
    public static boolean isDerivedMethod(JavaType mapper, JavaMethod method, Predicate<String> statementExists) {
        return isEnhancedMapper(mapper) && !method.isBridge() && !method.isDefault()
                && !hasStatement(mapper, method.getName(), statementExists, new HashSet<>());
    }

    private static boolean hasStatement(JavaType mapper, String method, Predicate<String> lookup, Set<JavaType> visited) {
        if (!visited.add(mapper)) {
            return false;
        }
        if (lookup.test(mapper.getName() + "." + method)) {
            return true;
        }
        for (JavaType parent : mapper.getGenericInterfaces()) {
            if (hasStatement(parent, method, lookup, visited)) {
                return true;
            }
        }
        return false;
    }
}
