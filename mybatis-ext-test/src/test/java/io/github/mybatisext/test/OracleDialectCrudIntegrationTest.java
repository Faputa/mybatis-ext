package io.github.mybatisext.test;

import io.github.mybatisext.dialect.OracleDialect;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class OracleDialectCrudIntegrationTest extends AbstractDialectCrudIntegrationTest {

    @Override
    protected DialectDataSource createDataSource() throws Exception {
        return newDataSource(new OracleDialect(), "Oracle");
    }

    @Override
    @Test
    @Disabled("H2 Oracle mode does not support Oracle anonymous blocks for batch statements")
    void savesUsersInBatchWithPerRowNullSemantics() {
    }

    @Override
    @Test
    @Disabled("H2 Oracle mode does not support Oracle anonymous blocks for batch statements")
    void updatesAndDeletesUsersInBatch() {
    }
}
