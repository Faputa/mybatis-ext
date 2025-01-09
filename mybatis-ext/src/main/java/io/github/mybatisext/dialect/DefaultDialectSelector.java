package io.github.mybatisext.dialect;

import io.github.mybatisext.exception.MybatisExtException;

public class DefaultDialectSelector implements DialectSelector {

    @Override
    public Dialect select(String jdbcUrl) {
        if (jdbcUrl.contains(":mysql:")) {
            return new MySqlDialect();
        }
        if (jdbcUrl.contains(":oracle:") || jdbcUrl.contains(":dm:")) {
            return new OracleDialect();
        }
        if (jdbcUrl.contains(":postgresql:")) {
            return new PostgreSqlDialect();
        }
        if (jdbcUrl.contains(":h2:")) {
            return new H2Dialect();
        }
        throw new MybatisExtException("Unsupported JDBC URL: " + jdbcUrl);
    }
}
