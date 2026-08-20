package io.github.mybatisext.test;

import io.github.mybatisext.dialect.PostgreSqlDialect;

class PostgreSqlDialectCrudIntegrationTest extends AbstractDialectCrudIntegrationTest {

    @Override
    protected DialectDataSource createDataSource() throws Exception {
        return newDataSource(new PostgreSqlDialect(), "PostgreSQL");
    }
}
