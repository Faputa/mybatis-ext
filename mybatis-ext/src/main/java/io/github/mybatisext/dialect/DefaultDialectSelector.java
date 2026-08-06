package io.github.mybatisext.dialect;

import io.github.mybatisext.exception.MybatisExtException;

public class DefaultDialectSelector implements DialectSelector {

    public static final MySqlDialect MYSQL_DIALECT = new MySqlDialect();
    public static final OracleDialect ORACLE_DIALECT = new OracleDialect();
    public static final DmDialect DM_DIALECT = new DmDialect();
    public static final PostgreSqlDialect POSTGRESQL_DIALECT = new PostgreSqlDialect();
    public static final H2Dialect H2_DIALECT = new H2Dialect();
    public static final SqlServerDialect SQL_SERVER_DIALECT = new SqlServerDialect();
    public static final SqlServer2008Dialect SQL_SERVER_2008_DIALECT = new SqlServer2008Dialect();

    @Override
    public Dialect select(String jdbcUrl) {
        if (jdbcUrl.contains(":mysql:")) {
            return MYSQL_DIALECT;
        }
        if (jdbcUrl.contains(":oracle:")) {
            return ORACLE_DIALECT;
        }
        if (jdbcUrl.contains(":dm:")) {
            return DM_DIALECT;
        }
        if (jdbcUrl.contains(":postgresql:")) {
            return POSTGRESQL_DIALECT;
        }
        if (jdbcUrl.contains(":h2:")) {
            return H2_DIALECT;
        }
        if (jdbcUrl.contains(":sqlserver:")) {
            return SQL_SERVER_DIALECT;
        }
        throw new MybatisExtException("Unsupported JDBC URL: " + jdbcUrl);
    }
}
