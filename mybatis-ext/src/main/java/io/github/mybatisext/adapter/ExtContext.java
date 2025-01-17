package io.github.mybatisext.adapter;

import io.github.mybatisext.dialect.DefaultDialectSelector;
import io.github.mybatisext.dialect.DialectSelector;

public class ExtContext {

    private boolean defaultFilterableEnable;
    private DialectSelector dialectSelector = new DefaultDialectSelector();

    public boolean isDefaultFilterableEnable() {
        return defaultFilterableEnable;
    }

    public void setDefaultFilterableEnable(boolean defaultFilterableEnable) {
        this.defaultFilterableEnable = defaultFilterableEnable;
    }

    public DialectSelector getDialectSelector() {
        return dialectSelector;
    }

    public void setDialectSelector(DialectSelector dialectSelector) {
        this.dialectSelector = dialectSelector;
    }
}
