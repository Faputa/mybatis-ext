package io.github.mybatisext.jpa;

import java.util.Arrays;
import java.util.Objects;
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
                if (symbol.match(new State(state, tokenizer.getCursor()), continuation)) {
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
            return head.match(new State(state, tokenizer.getCursor()), tail == null ? continuation : s2 -> {
                return tail.match(new State(state, tokenizer.getCursor()), continuation);
            });
        });
    }

    protected Symbol optional(Symbol symbol) {
        Symbol optional = new Symbol("optional(" + symbol + ")");
        return optional.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            int cursor = tokenizer.getCursor();
            if (symbol.match(new State(state, tokenizer.getCursor()), continuation)) {
                return true;
            }
            tokenizer.setCursor(cursor);
            return continuation.test(state);
        });
    }

    protected Symbol star(Symbol symbol) {
        Symbol star = new Symbol("star(" + symbol + ")");
        return star.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            int cursor = tokenizer.getCursor();
            if (symbol.match(new State(state, tokenizer.getCursor()), s2 -> {
                return star.match(new State(state, tokenizer.getCursor()), continuation);
            })) {
                return true;
            }
            tokenizer.setCursor(cursor);
            return continuation.test(state);
        });
    }

    protected Symbol plus(Symbol symbol) {
        Symbol plus = new Symbol("plus(" + symbol + ")");
        Symbol star = star(symbol);
        return plus.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            return symbol.match(new State(state, tokenizer.getCursor()), s2 -> {
                return star.match(new State(state, tokenizer.getCursor()), continuation);
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
            return symbol.match(new State(state, tokenizer.getCursor()), countDec == null ? continuation : s2 -> {
                return countDec.match(new State(state, tokenizer.getCursor()), continuation);
            });
        });
    }

    protected Symbol assign(String name, Symbol symbol) {
        Symbol assign = new Symbol("assign(" + name + "," + symbol + ")");
        return assign.setLeftSymbols(symbol).setMatch((state, continuation) -> {
            int begin = tokenizer.getCursor();
            return symbol.match(new State(state, tokenizer.getCursor()), s2 -> {
                int end = tokenizer.getCursor();
                Objects.requireNonNull(state.getParent(), "Prohibited: setting variables on top-level symbols.").set(name, tokenizer.substring(begin, end));
                return continuation.test(s2);
            });
        });
    }
}
