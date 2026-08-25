package io.github.mybatisext.adapter;

import io.github.mybatisext.dialect.DefaultDialectSelector;
import io.github.mybatisext.dialect.DialectSelector;

public class ExtContext {

    private boolean defaultFilterable = true;
    private DialectSelector dialectSelector = new DefaultDialectSelector();

    public boolean isDefaultFilterable() {
        return defaultFilterable;
    }

    public void setDefaultFilterable(boolean defaultFilterable) {
        this.defaultFilterable = defaultFilterable;
    }

    public DialectSelector getDialectSelector() {
        return dialectSelector;
    }

    public void setDialectSelector(DialectSelector dialectSelector) {
        this.dialectSelector = dialectSelector;
    }
}
