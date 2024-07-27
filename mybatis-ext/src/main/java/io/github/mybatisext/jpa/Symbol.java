package io.github.mybatisext.jpa;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Symbol {

    private final String name;
    private final Set<Symbol> leftSymbols = new HashSet<>();
    private Match match = (state, continuation) -> continuation.test(state);

    public Symbol(String name) {
        this.name = name;
    }

    public Symbol setMatch(Match match) {
        this.match = match;
        return this;
    }

    public Symbol setLeftSymbols(Symbol... symbols) {
        leftSymbols.clear();
        leftSymbols.addAll(Arrays.asList(symbols));
        return this;
    }

    public Symbol set(Symbol symbol) {
        if (symbol.hasLeftRecursion(this)) {
            throw new ParserException("Left recursion detected in " + name);
        }
        this.match = symbol::match;
        return setLeftSymbols(symbol);
    }

    public boolean hasLeftRecursion(Symbol symbol) {
        if (leftSymbols.contains(symbol)) {
            return true;
        }
        for (Symbol leftSymbol : leftSymbols) {
            if (leftSymbol.hasLeftRecursion(symbol)) {
                return true;
            }
        }
        return false;
    }

    public boolean match(State state, Continuation continuation) {
        return match.test(state, continuation);
    }

    @Override
    public String toString() {
        return name;
    }
}
