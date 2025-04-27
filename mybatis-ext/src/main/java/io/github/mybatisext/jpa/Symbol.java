package io.github.mybatisext.jpa;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Symbol<T extends Tokenizer> {

    private final String name;
    private final Set<Symbol<T>> leftSymbols = new HashSet<>();
    private Match<T> match = (state, continuation) -> continuation.test(state);

    public Symbol(String name) {
        this.name = name;
    }

    public Symbol<T> setMatch(Match<T> match) {
        this.match = match;
        return this;
    }

    @SafeVarargs
    public final Symbol<T> setLeftSymbols(Symbol<T>... symbols) {
        leftSymbols.clear();
        leftSymbols.addAll(Arrays.asList(symbols));
        return this;
    }

    public Symbol<T> set(Match<T> match) {
        this.match = (state, continuation) -> {
            Tokenizer tokenizer = state.getTokenizer();
            int begin = tokenizer.getCursor();
            return match.test(state, s2 -> {
                int end = tokenizer.getCursor();
                s2.addMatch(this, state.getScope(), tokenizer.substring(begin, end), s2.getResult());
                return continuation.test(state);
            });
        };
        return this;
    }

    public Symbol<T> set(Symbol<T> symbol) {
        if (symbol.hasLeftRecursion(this)) {
            throw new ParserException("Left recursion detected in " + name);
        }
        this.match = (state, continuation) -> {
            Tokenizer tokenizer = state.getTokenizer();
            int begin = tokenizer.getCursor();
            return symbol.match(new State<>(state, new Scope<>(name, state)), s2 -> {
                int end = tokenizer.getCursor();
                Object value = s2.getReturn();
                if (value == null) {
                    value = s2.getResult();
                }
                s2.addMatch(this, state.getScope(), tokenizer.substring(begin, end), value);
                s2.setResult(value);
                return continuation.test(s2);
            });
        };
        return setLeftSymbols(symbol);
    }

    public boolean hasLeftRecursion(Symbol<T> symbol) {
        if (leftSymbols.contains(symbol)) {
            return true;
        }
        for (Symbol<T> leftSymbol : leftSymbols) {
            if (leftSymbol.hasLeftRecursion(symbol)) {
                return true;
            }
        }
        return false;
    }

    public boolean match(T tokenizer, Continuation<T> continuation) {
        return match(new State<>(tokenizer), continuation);
    }

    public boolean match(State<T> state, Continuation<T> continuation) {
        return match.test(state, continuation);
    }

    @Override
    public String toString() {
        return name;
    }
}
