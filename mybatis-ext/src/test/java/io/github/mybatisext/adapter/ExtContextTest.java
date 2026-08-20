package io.github.mybatisext.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.mybatisext.dialect.DefaultDialectSelector;
import io.github.mybatisext.dialect.DialectSelector;

class ExtContextTest {

    @Test
    void providesUsableDefaults() {
        ExtContext extContext = new ExtContext();

        assertTrue(extContext.isDefaultFilterable());
        assertTrue(extContext.getDialectSelector() instanceof DefaultDialectSelector);
    }

    @Test
    void appliesExplicitOptions() {
        ExtContext extContext = new ExtContext();
        DialectSelector dialectSelector = jdbcUrl -> DefaultDialectSelector.H2_DIALECT;

        extContext.setDefaultFilterable(false);
        extContext.setDialectSelector(dialectSelector);

        assertFalse(extContext.isDefaultFilterable());
        assertSame(dialectSelector, extContext.getDialectSelector());
    }
}
