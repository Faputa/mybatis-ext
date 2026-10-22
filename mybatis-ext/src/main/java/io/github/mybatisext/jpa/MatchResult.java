package io.github.mybatisext.jpa;

public class MatchResult {

    // 坐标
    private final Symbol<?> symbol;
    private final Scope<?> scope;
    // 值
    private final String text;
    private final Object value;
    private final int start;
    private final int end;

    public MatchResult(Symbol<?> symbol, Scope<?> scope, String text, Object value, int start, int end) {
        this.symbol = symbol;
        this.scope = scope;
        this.text = text;
        this.value = value;
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public Scope<?> getScope() {
        return scope;
    }

    public Symbol<?> getSymbol() {
        return symbol;
    }

    public String text() {
        return text;
    }

    @SuppressWarnings("unchecked")
    public <V> V val() {
        return (V) value;
    }
}
