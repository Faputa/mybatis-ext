package io.github.mybatisext.dialect;

public interface DialectSelector {

    Dialect select(String jdbcUrl);
}
