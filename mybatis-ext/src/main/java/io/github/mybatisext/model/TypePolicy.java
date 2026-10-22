package io.github.mybatisext.model;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import io.github.mybatisext.diagnostic.DiagnosticLocation;

public interface TypePolicy {

    enum ScalarKind {
        SCALAR, OBJECT, UNKNOWN
    }

    ScalarKind classify(JavaType type);

    default boolean isScalar(JavaType type) {
        ScalarKind kind = classify(type);
        if (kind == ScalarKind.UNKNOWN) {
            throw new UnresolvedTypeException(type.getName());
        }
        return kind == ScalarKind.SCALAR;
    }

    default boolean isScalar(JavaType type, DiagnosticLocation location) {
        try {
            return isScalar(type);
        } catch (UnresolvedTypeException e) {
            throw e.at(location);
        }
    }

    default boolean isSpecialParameter(JavaType type) {
        return JavaTypes.isAssignableTo(type, RowBounds.class.getName())
                || JavaTypes.isAssignableTo(type, ResultHandler.class.getName());
    }
}
