package io.github.mybatisext.jpa;

@FunctionalInterface
interface Continuation {
    boolean test(State state);
}

@FunctionalInterface
interface Match {
    boolean test(State state, Continuation continuation);
}

public final class Symbol {

    private final String name;
    private Match match;

    public Symbol(String name) {
        this.name = name;
    }

    public Symbol setMatch(Match match) {
        this.match = match;
        return this;
    }

    public Symbol set(Symbol symbol) {
        this.match = symbol::match;
        return this;
    }

    public boolean match(State state, Continuation continuation) {
        return match.test(state, continuation);
    }

    @Override
    public String toString() {
        return name;
    }
}
