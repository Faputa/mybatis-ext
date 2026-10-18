package io.github.mybatisext.reflect;

import java.lang.reflect.Modifier;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.diagnostic.DiagnosticLocation;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.TypePolicy;

public final class ReflectionTypePolicy implements TypePolicy {

    private final Configuration configuration;

    public ReflectionTypePolicy(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ScalarKind classify(JavaType type) {
        if (!(type instanceof GenericType) || !type.isResolved()) {
            return ScalarKind.UNKNOWN;
        }
        return configuration.getTypeHandlerRegistry().hasTypeHandler(((GenericType) type).getType()) ? ScalarKind.SCALAR : ScalarKind.OBJECT;
    }

    /** 仅运行时语句与结果映射可能需要实际的 Class。 */
    public static Class<?> requireClass(JavaType type) {
        if (!(type instanceof GenericType)) {
            throw new IllegalArgumentException("Runtime mapping requires a reflection type: " + type.getName());
        }
        return ((GenericType) type).getType();
    }

    public static void validateIdGenerator(JavaType type, DiagnosticLocation location) {
        Class<?> generator = requireClass(type);
        try {
            if (!Modifier.isPublic(generator.getModifiers())
                    || Modifier.isAbstract(generator.getModifiers())
                    || !Modifier.isPublic(generator.getConstructor().getModifiers())) {
                throw new NoSuchMethodException(generator.getName());
            }
        } catch (NoSuchMethodException e) {
            throw new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, "customIdGenerator cannot be instantiated", location), e);
        }
    }
}
