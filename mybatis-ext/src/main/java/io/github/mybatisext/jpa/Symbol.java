package io.github.mybatisext.jpa;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Symbol {

    private final String name;
    private final Set<Symbol> leftSymbols = new HashSet<>();
    private Match match = (state, tokenizer, continuation) -> continuation.test(state);

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
        this.match = (state, tokenizer, continuation) -> {
            Scope scope = new Scope(name, state);
            int begin = tokenizer.getCursor();
            return symbol.match(new State(state, scope), tokenizer, s2 -> {
                int end = tokenizer.getCursor();
                Object value = scope.getReturnValue() != null ? scope.getReturnValue() : s2.getResult();
                s2.addMatch(this, state.getScope(), tokenizer.substring(begin, end), value);
                s2.setResult(value);
                return continuation.test(s2);
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

    public boolean match(Tokenizer tokenizer, Continuation continuation) {
        return match.test(new State(), tokenizer, continuation);
    }

    public boolean match(State state, Tokenizer tokenizer, Continuation continuation) {
        return match.test(state, tokenizer, continuation);
    }

    @Override
    public String toString() {
        return name;
    }
}
