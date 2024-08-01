package io.github.mybatisext.jpa;

public class MatchResult {

    // 坐标
    private final Symbol symbol;
    private final Scope scope;
    // 值
    private final String text;
    private final Object value;

    public MatchResult(Symbol symbol, Scope scope, String text, Object value) {
        this.symbol = symbol;
        this.scope = scope;
        this.text = text;
        this.value = value;
    }

    public Scope getScope() {
        return scope;

    }

    public Symbol getSymbol() {
        return symbol;
    }

    public String text() {
        return text;
    }

    @SuppressWarnings("unchecked")
    public <T> T val() {
        return (T) value;
    }
}
