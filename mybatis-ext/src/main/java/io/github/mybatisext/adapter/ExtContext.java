package io.github.mybatisext.adapter;

import io.github.mybatisext.dialect.DefaultDialectSelector;
import io.github.mybatisext.dialect.DialectSelector;

public class ExtContext {

    private DialectSelector dialectSelector = new DefaultDialectSelector();

    public DialectSelector getDialectSelector() {
        return dialectSelector;
    }

    public void setDialectSelector(DialectSelector dialectSelector) {
        this.dialectSelector = dialectSelector;
    }
}
