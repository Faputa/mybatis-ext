package io.github.mybatisext.metadata;

import java.util.Map;
import java.util.function.Function;

import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.diagnostic.DiagnosticLocation;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.model.JavaType;

/** 统一查找声明属性与已解析属性，仅在请求时收集引用。 */
public final class PropertyPath {

    private PropertyPath() {
    }

    @FunctionalInterface
    public interface SegmentConsumer<T> {

        void accept(T property, int start, int end);
    }

    public static <T> T resolve(Map<String, T> properties, String path, JavaType targetType, Function<T, Map<String, T>> children,
            DiagnosticLocation location, SegmentConsumer<T> onSegment) {
        T property = null;
        int start = 0;
        for (String segment : path.split("\\.", -1)) {
            property = properties == null ? null : properties.get(segment);
            if (property == null) {
                String message = "Property '" + path + "' not found"
                        + " in " + targetType.getSimpleName();
                throw new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, message, location.pathRange(start, start + segment.length())));
            }
            if (onSegment != null) {
                onSegment.accept(property, start, start + segment.length());
            }
            properties = children.apply(property);
            start += segment.length() + 1;
        }
        return property;
    }
}
