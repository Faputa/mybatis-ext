package io.github.mybatisext.exception;

public class MybatisExtException extends RuntimeException {

    public MybatisExtException(String message) {
        super(message);
    }

    public MybatisExtException(String message, Throwable cause) {
        super(message, cause);
    }

    public MybatisExtException(Throwable cause) {
        super(cause);
    }
}
