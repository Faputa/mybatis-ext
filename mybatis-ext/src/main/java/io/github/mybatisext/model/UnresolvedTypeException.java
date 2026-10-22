package io.github.mybatisext.model;

import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.diagnostic.DiagnosticLocation;
import io.github.mybatisext.exception.MybatisExtException;

public class UnresolvedTypeException extends MybatisExtException {

    public UnresolvedTypeException(String name) {
        super(new Diagnostic(Diagnostic.Code.UNRESOLVED_TYPE, "Unresolved type: " + name, null));
    }

    private UnresolvedTypeException(Diagnostic diagnostic, Throwable cause) {
        super(diagnostic, cause);
    }

    /** 补充缺失的声明上下文，不修改原异常。 */
    public UnresolvedTypeException at(DiagnosticLocation location) {
        Diagnostic diagnostic = getDiagnostic();
        return diagnostic.getLocation() != null
                ? this
                : new UnresolvedTypeException(
                        new Diagnostic(diagnostic.getCode(), diagnostic.getMessage(), location), this);
    }
}
