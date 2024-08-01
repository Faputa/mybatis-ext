package io.github.mybatisext.jpa;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseParser<T extends Tokenizer> {

    protected final T tokenizer;

    protected BaseParser(T tokenizer) {
        this.tokenizer = tokenizer;
    }

    protected Symbol choice(Symbol... symbols) {
        Symbol choice = new Symbol("choice(" + Stream.of(symbols).map(Symbol::toString).collect(Collectors.joining(",")) + ")");
        return choice.setLeftSymbols(symbols).setMatch((state, continuation) -> {
            int cursor = tokenizer.getCursor();
            for (Symbol symbol : symbols) {
                if (symbol.match(new State(state, state.getScope(), tokenizer.getCursor()), continuation)) {
                    return true;
                }
                tokenizer.setCursor(cursor);
            }
            return false;
        });
    }

    protected Symbol join(Symbol... symbols) {
        Symbol join = new Symbol("join(" + Stream.of(symbols).map(Symbol::toString).collect(Collectors.joining(",")) + ")");
        if (symbols.length == 0) {
            return join;
        }
        Symbol head = symbols[0];
        Symbol tail = symbols.length == 2 ? symbols[1] : symbols.length > 2 ? join(Arrays.copyOfRange(symbols, 1, symbols.length)) : null;
        return join.setLeftSymbols(head).setMatch((state, continuation) -> {
            return head.match(new State(state, state.getScope(), tokenizer.getCursor()), tail == null ? continuation : (s2, result) -> {
                return tail.match(new State(s2, state.getScope(), tokenizer.getCursor()), continuation);
            });
        });
    }

    protected Symbol optional(Symbol symbol) {
        Symbol optional = new Symbol("optional(" + symbol + ")");
        return optional.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            int cursor = tokenizer.getCursor();
            if (symbol.match(new State(state, state.getScope(), tokenizer.getCursor()), continuation)) {
                return true;
            }
            tokenizer.setCursor(cursor);
            return continuation.test(state, null);
        });
    }

    private boolean matchStar(Symbol symbol, State state, Continuation continuation, Object result) {
        int cursor = tokenizer.getCursor();
        if (symbol.match(new State(state, state.getScope(), tokenizer.getCursor()), (s2, r2) -> {
            return matchStar(symbol, s2, continuation, r2);
        })) {
            return true;
        }
        tokenizer.setCursor(cursor);
        return continuation.test(state, result);
    }

    protected Symbol star(Symbol symbol) {
        Symbol star = new Symbol("star(" + symbol + ")");
        return star.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            return matchStar(symbol, state, continuation, null);
        });
    }

    protected Symbol plus(Symbol symbol) {
        Symbol plus = new Symbol("plus(" + symbol + ")");
        return plus.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            return symbol.match(new State(state, state.getScope(), tokenizer.getCursor()), (s2, result) -> {
                return matchStar(symbol, s2, continuation, result);
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
            return symbol.match(new State(state, state.getScope(), tokenizer.getCursor()), countDec == null ? continuation : (s2, result) -> {
                return countDec.match(new State(state, state.getScope(), tokenizer.getCursor()), continuation);
            });
        });
    }

    protected Symbol assign(String name, Symbol symbol) {
        Symbol assign = new Symbol("assign(" + name + "," + symbol + ")");
        return assign.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            int begin = tokenizer.getCursor();
            return symbol.match(new State(state, state.getScope(), tokenizer.getCursor()), (s2, result) -> {
                int end = tokenizer.getCursor();
                s2.addMatch(name, symbol, state.getScope(), tokenizer.substring(begin, end), result);
                return continuation.test(s2, result);
            });
        });
    }

    protected Symbol action(Predicate<State> predicate) {
        Symbol action = new Symbol("action(" + predicate + ")");
        return action.setMatch((state, continuation) -> {
            return predicate.test(state) && continuation.test(state, null);
        });
    }

    protected Symbol action(Consumer<State> consumer) {
        Symbol action = new Symbol("action(" + consumer + ")");
        return action.setMatch((state, continuation) -> {
            consumer.accept(state);
            return continuation.test(state, null);
        });
    }
}
