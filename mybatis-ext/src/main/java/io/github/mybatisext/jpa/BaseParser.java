package io.github.mybatisext.jpa;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseParser<T extends Tokenizer> {

    public class Symbol extends io.github.mybatisext.jpa.Symbol<T> {
        public Symbol(String name) {
            super(name);
        }

        public Symbol set(Match<T> match) {
            super.set(match);
            return this;
        }

        public Symbol set(Symbol symbol) {
            super.set(symbol);
            return this;
        }

        @SafeVarargs
        public final Symbol setLeftSymbols(Symbol... symbols) {
            super.setLeftSymbols(symbols);
            return this;
        }

        public Symbol setMatch(Match<T> match) {
            super.setMatch(match);
            return this;
        }
    }

    @SafeVarargs
    protected final Symbol choice(Symbol... symbols) {
        Symbol choice = new Symbol("choice(" + Stream.of(symbols).map(Symbol::toString).collect(Collectors.joining(",")) + ")");
        return choice.setLeftSymbols(symbols).setMatch((state, continuation) -> {
            Tokenizer tokenizer = state.getTokenizer();
            int cursor = tokenizer.getCursor();
            for (Symbol symbol : symbols) {
                if (symbol.match(new State<>(state, state.getScope()), continuation)) {
                    return true;
                }
                tokenizer.setCursor(cursor);
            }
            return false;
        });
    }

    @SafeVarargs
    protected final Symbol join(Symbol... symbols) {
        Symbol join = new Symbol("join(" + Stream.of(symbols).map(Symbol::toString).collect(Collectors.joining(",")) + ")");
        if (symbols.length == 0) {
            return join;
        }
        Symbol head = symbols[0];
        Symbol tail = symbols.length == 2 ? symbols[1] : symbols.length > 2 ? join(Arrays.copyOfRange(symbols, 1, symbols.length)) : null;
        return join.setLeftSymbols(head).setMatch((state, continuation) -> {
            return head.match(new State<>(state, state.getScope()), tail == null ? continuation : s2 -> {
                return tail.match(new State<>(s2, state.getScope()), continuation);
            });
        });
    }

    protected Symbol optional(Symbol symbol) {
        Symbol optional = new Symbol("optional(" + symbol + ")");
        return optional.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            Tokenizer tokenizer = state.getTokenizer();
            int cursor = tokenizer.getCursor();
            if (symbol.match(new State<>(state, state.getScope()), continuation)) {
                return true;
            }
            tokenizer.setCursor(cursor);
            return continuation.test(state);
        });
    }

    private boolean matchStar(Symbol symbol, State<T> state, Continuation<T> continuation) {
        Tokenizer tokenizer = state.getTokenizer();
        int cursor = tokenizer.getCursor();
        if (symbol.match(new State<>(state, state.getScope()), s2 -> {
            return matchStar(symbol, s2, continuation);
        })) {
            return true;
        }
        tokenizer.setCursor(cursor);
        return continuation.test(state);
    }

    protected Symbol star(Symbol symbol) {
        Symbol star = new Symbol("star(" + symbol + ")");
        return star.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            return matchStar(symbol, state, continuation);
        });
    }

    protected Symbol plus(Symbol symbol) {
        Symbol plus = new Symbol("plus(" + symbol + ")");
        return plus.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            return symbol.match(new State<>(state, state.getScope()), s2 -> {
                return matchStar(symbol, s2, continuation);
            });
        });
    }

    protected Symbol count(Symbol symbol, int num) {
        Symbol count = new Symbol("count(" + symbol + "," + num + ")");
        if (num < 1) {
            return count;
        }
        Symbol countDec = num > 1 ? count(symbol, num - 1) : null;
        return count.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            return symbol.match(new State<>(state, state.getScope()), countDec == null ? continuation : s2 -> {
                return countDec.match(new State<>(s2, state.getScope()), continuation);
            });
        });
    }

    protected Symbol assign(String name, Symbol symbol) {
        Symbol assign = new Symbol("assign(" + name + "," + symbol + ")");
        return assign.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            Tokenizer tokenizer = state.getTokenizer();
            int begin = tokenizer.getCursor();
            return symbol.match(new State<>(state, state.getScope()), s2 -> {
                int end = tokenizer.getCursor();
                s2.addMatch(name, symbol, state.getScope(), tokenizer.substring(begin, end), s2.getResult());
                return continuation.test(s2);
            });
        });
    }

    protected Symbol action(Predicate<State<T>> predicate) {
        Symbol action = new Symbol("action(" + predicate.hashCode() + ")");
        return action.setMatch((state, continuation) -> {
            return predicate.test(state) && continuation.test(state);
        });
    }

    protected Symbol action(Consumer<State<T>> consumer) {
        Symbol action = new Symbol("action(" + consumer.hashCode() + ")");
        return action.setMatch((state, continuation) -> {
            consumer.accept(state);
            return continuation.test(state);
        });
    }
}
