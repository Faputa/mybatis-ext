package io.github.mybatisext.test;

import io.github.mybatisext.dialect.DmDialect;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class DmDialectCrudIntegrationTest extends AbstractDialectCrudIntegrationTest {

    @Override
    protected DialectDataSource createDataSource() throws Exception {
        return newDataSource(new DmDialect(), "Oracle");
    }

    @Override
    @Test
    @Disabled("H2 Oracle mode does not support DM anonymous blocks for batch statements")
    void savesUsersInBatchWithPerRowNullSemantics() {
    }

    @Override
    @Test
    @Disabled("H2 Oracle mode does not support DM anonymous blocks for batch statements")
    void updatesAndDeletesUsersInBatch() {
    }
}
