package io.github.mybatisext.diagnostic;

import java.io.Serializable;

/** 不可变的问题描述，以及可用时与源码绑定的位置信息。 */
public final class Diagnostic implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Code {
        SYNTAX_ERROR, AMBIGUOUS, SEMANTIC_ERROR, UNRESOLVED_TYPE
    }

    private final Code code;
    private final String message;
    private final DiagnosticLocation location;

    public Diagnostic(Code code, String message, DiagnosticLocation location) {
        this.code = code;
        this.message = message;
        this.location = location;
    }

    public Code getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public DiagnosticLocation getLocation() {
        return location;
    }

    public boolean hasRange() {
        return location != null && location.hasRange();
    }

    public int getStart() {
        return hasRange() ? location.getStart() : -1;
    }

    public int getEnd() {
        return hasRange() ? location.getEnd() : -1;
    }

    public String format() {
        return location == null ? message : message + " [" + location + "]";
    }
}
