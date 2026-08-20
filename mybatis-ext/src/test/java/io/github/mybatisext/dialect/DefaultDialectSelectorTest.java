package io.github.mybatisext.dialect;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.github.mybatisext.exception.MybatisExtException;

class DefaultDialectSelectorTest {

    private final DefaultDialectSelector selector = new DefaultDialectSelector();

    @Test
    void selectsSupportedDialects() {
        assertSame(DefaultDialectSelector.MYSQL_DIALECT, selector.select("jdbc:mysql://localhost/test"));
        assertSame(DefaultDialectSelector.ORACLE_DIALECT, selector.select("jdbc:oracle:thin:@localhost:1521:test"));
        assertSame(DefaultDialectSelector.DM_DIALECT, selector.select("jdbc:dm://localhost:5236/test"));
        assertSame(DefaultDialectSelector.POSTGRESQL_DIALECT, selector.select("jdbc:postgresql://localhost/test"));
        assertSame(DefaultDialectSelector.H2_DIALECT, selector.select("jdbc:h2:mem:test"));
        assertSame(DefaultDialectSelector.SQL_SERVER_DIALECT, selector.select("jdbc:sqlserver://localhost:1433;databaseName=test"));
    }

    @Test
    void rejectsUnsupportedDialect() {
        assertThrows(MybatisExtException.class, () -> selector.select("jdbc:sqlite:test.db"));
    }
}
