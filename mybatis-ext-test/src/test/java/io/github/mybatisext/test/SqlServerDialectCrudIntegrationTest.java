package io.github.mybatisext.test;

import io.github.mybatisext.dialect.SqlServerDialect;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SqlServerDialectCrudIntegrationTest extends AbstractDialectCrudIntegrationTest {

    @Override
    protected DialectDataSource createDataSource() throws Exception {
        return newDataSource(new SqlServerDialect(), "MSSQLServer");
    }

    @Override
    @Test
    @Disabled("H2 MSSQL mode does not support SQL Server alias update syntax")
    void updatesUser() {
    }

    @Override
    @Test
    @Disabled("H2 MSSQL mode does not support SQL Server alias delete syntax")
    void deletesUser() {
    }

    @Override
    @Test
    @Disabled("H2 MSSQL mode does not support SQL Server alias update syntax")
    void appliesSaveAndUpdateNullSemantics() {
    }

    @Override
    @Test
    @Disabled("H2 MSSQL mode does not support SQL Server alias update and delete syntax")
    void updatesAndDeletesUsersInBatch() {
    }
}
