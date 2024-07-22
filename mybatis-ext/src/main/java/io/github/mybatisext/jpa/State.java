package io.github.mybatisext.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class State {

    private final Symbol symbol;
    private final @Nullable State parent;
    private final int cursor;
    private final Map<String, Object> variable = new HashMap<>();

    public State(Symbol symbol) {
        this(symbol, null, 0);
    }

    public State(Symbol symbol, @Nullable State parent, int cursor) {
        this.symbol = symbol;
        this.parent = parent;
        this.cursor = cursor;
    }

    @SuppressWarnings("null")
    public boolean hasLeftRecursion(Symbol symbol) {
        if (parent == null) {
            return false;
        }
        if (parent.symbol == symbol && parent.cursor == cursor) {
            return true;
        }
        return parent.hasLeftRecursion(symbol);
    }

    public @Nullable State getParent() {
        return parent;
    }

    public int getCursor() {
        return cursor;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        return (T) (variable.get(name));
    }

    public <T> void set(String name, T t) {
        variable.put(name, t);
    }
}
