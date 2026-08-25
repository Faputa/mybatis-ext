package io.github.mybatisext.adapter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.mybatisext.dialect.DefaultDialectSelector;
import io.github.mybatisext.dialect.DialectSelector;

public class ExtContext {

    private boolean defaultFilterable = true;
    private DialectSelector dialectSelector = new DefaultDialectSelector();
    // 本框架生成的statement id，用于与用户自定义语句（注解/XML）区分
    private final Set<String> generatedStatementIds = ConcurrentHashMap.newKeySet();

    public boolean isGeneratedStatement(String statementId) {
        return generatedStatementIds.contains(statementId);
    }

    public void markStatementGenerated(String statementId) {
        generatedStatementIds.add(statementId);
    }

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
