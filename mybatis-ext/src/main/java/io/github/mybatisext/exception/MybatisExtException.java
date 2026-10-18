package io.github.mybatisext.exception;

import io.github.mybatisext.diagnostic.Diagnostic;

public class MybatisExtException extends RuntimeException {

    private final Diagnostic diagnostic;

    public MybatisExtException(String message) {
        super(message);
        this.diagnostic = null;
    }

    public MybatisExtException(String message, Throwable cause) {
        super(message, cause);
        this.diagnostic = null;
    }

    public MybatisExtException(Throwable cause) {
        super(cause);
        this.diagnostic = null;
    }

    public MybatisExtException(Diagnostic diagnostic) {
        super(diagnostic.format());
        this.diagnostic = diagnostic;
    }

    public MybatisExtException(Diagnostic diagnostic, Throwable cause) {
        super(diagnostic.format(), cause);
        this.diagnostic = diagnostic;
    }

    /** 非语义分析错误时为空。 */
    public Diagnostic getDiagnostic() {
        return diagnostic;
    }
}
