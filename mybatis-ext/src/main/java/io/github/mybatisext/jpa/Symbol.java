package io.github.mybatisext.jpa;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Symbol {

    private final String name;
    private final Set<Symbol> leftSymbols = new HashSet<>();
    private Match match = (state, result, continuation) -> continuation.test(state, result);

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
        this.match = (state, result, continuation) -> {
            Scope scope = new Scope(name);
            Tokenizer tokenizer = state.getTokenizer();
            int begin = tokenizer.getCursor();
            return symbol.match(new State(state, scope, tokenizer), result, (s2, r2) -> {
                int end = tokenizer.getCursor();
                Object value = scope.getReturnValue() != null ? scope.getReturnValue() : r2;
                s2.addMatch(this, state.getScope(), tokenizer.substring(begin, end), value);
                return continuation.test(s2, value);
            });
        };
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

    public boolean match(State state, Object result, Continuation continuation) {
        return match.test(state, result, continuation);
    }

    @Override
    public String toString() {
        return name;
    }
}
