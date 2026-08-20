package io.github.mybatisext.test;

import io.github.mybatisext.dialect.MySqlDialect;

class MySqlDialectCrudIntegrationTest extends AbstractDialectCrudIntegrationTest {

    @Override
    protected DialectDataSource createDataSource() throws Exception {
        return newDataSource(new MySqlDialect(), "MySQL");
    }
}
