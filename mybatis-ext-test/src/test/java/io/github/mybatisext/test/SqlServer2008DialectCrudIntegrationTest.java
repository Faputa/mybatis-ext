package io.github.mybatisext.test;

import io.github.mybatisext.dialect.SqlServer2008Dialect;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SqlServer2008DialectCrudIntegrationTest extends AbstractDialectCrudIntegrationTest {

    @Override
    protected DialectDataSource createDataSource() throws Exception {
        return newDataSource(new SqlServer2008Dialect(), "MSSQLServer");
    }

    @Override
    @Test
    @Disabled("H2 MSSQL mode does not support SQL Server 2008 alias update syntax")
    void updatesUser() {
    }

    @Override
    @Test
    @Disabled("H2 MSSQL mode does not support SQL Server 2008 alias delete syntax")
    void deletesUser() {
    }

    @Override
    @Test
    @Disabled("H2 MSSQL mode does not support SQL Server 2008 alias update syntax")
    void appliesSaveAndUpdateNullSemantics() {
    }

    @Override
    @Test
    @Disabled("H2 MSSQL mode does not support SQL Server 2008 alias update and delete syntax")
    void updatesAndDeletesUsersInBatch() {
    }
}
