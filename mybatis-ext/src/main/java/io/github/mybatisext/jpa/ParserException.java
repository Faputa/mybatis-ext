package io.github.mybatisext.jpa;

import io.github.mybatisext.diagnostic.Diagnostic;

public class ParserException extends RuntimeException {

    private final Diagnostic diagnostic;

    public ParserException(String message) {
        super(message);
        this.diagnostic = null;
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
        this.diagnostic = null;
    }

    public ParserException(Throwable cause) {
        super(cause);
        this.diagnostic = null;
    }

    public ParserException(Diagnostic diagnostic) {
        super(diagnostic.format());
        this.diagnostic = diagnostic;
    }

    public ParserException(Diagnostic diagnostic, Throwable cause) {
        super(diagnostic.format(), cause);
        this.diagnostic = diagnostic;
    }

    /** 非语义分析错误时为空。 */
    public Diagnostic getDiagnostic() {
        return diagnostic;
    }
}
