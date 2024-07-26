package io.github.mybatisext.jpa;

import java.util.function.Predicate;

public final class Symbol {

    private final String name;
    private Predicate<State> match;

    public Symbol(String name) {
        this.name = name;
    }

    public Symbol setMatch(Predicate<State> match) {
        this.match = match;
        return this;
    }

    public Symbol set(Symbol symbol) {
        this.match = state -> symbol.match(state);
        return this;
    }

    public boolean match(State state) {
        return match.test(state);
    }

    @Override
    public String toString() {
        return name;
    }
}
