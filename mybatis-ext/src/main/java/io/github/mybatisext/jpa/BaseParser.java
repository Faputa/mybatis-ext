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
        return choice.setMatch((state, continuation) -> {
            if (state.hasLeftRecursion(choice, tokenizer.getCursor())) {
                return false;
            }
            int cursor = tokenizer.getCursor();
            for (Symbol symbol : symbols) {
                if (symbol.match(new State(symbol, state, tokenizer.getCursor()), continuation)) {
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
        return join.setMatch((state, continuation) -> {
            if (state.hasLeftRecursion(join, tokenizer.getCursor())) {
                return false;
            }
            return head.match(new State(head, state, tokenizer.getCursor()), tail == null ? continuation : s2 -> {
                return tail.match(new State(tail, state, tokenizer.getCursor()), continuation);
            });
        });
    }

    protected Symbol optional(Symbol symbol) {
        Symbol optional = new Symbol("optional(" + symbol + ")");
        return optional.setMatch((state, continuation) -> {
            if (state.hasLeftRecursion(optional, tokenizer.getCursor())) {
                return false;
            }
            int cursor = tokenizer.getCursor();
            if (symbol.match(new State(symbol, state, tokenizer.getCursor()), continuation)) {
                return true;
            }
            tokenizer.setCursor(cursor);
            return continuation.test(state);
        });
    }

    protected Symbol star(Symbol symbol) {
        Symbol star = new Symbol("star(" + symbol + ")");
        return star.setMatch((state, continuation) -> {
            if (state.hasLeftRecursion(star, tokenizer.getCursor())) {
                return false;
            }
            int cursor = tokenizer.getCursor();
            if (symbol.match(new State(symbol, state, tokenizer.getCursor()), s2 -> {
                return star.match(new State(symbol, state, tokenizer.getCursor()), continuation);
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
        return plus.setMatch((state, continuation) -> {
            if (state.hasLeftRecursion(plus, tokenizer.getCursor())) {
                return false;
            }
            return symbol.match(new State(symbol, state, tokenizer.getCursor()), s2 -> {
                return star.match(new State(symbol, state, tokenizer.getCursor()), continuation);
            });
        });
    }

    protected Symbol count(Symbol symbol, int num) {
        Symbol count = new Symbol("count(" + symbol + "," + num + ")");
        if (num < 1) {
            return count;
        }
        if (num == 1) {
            return count.setMatch((state, continuation) -> {
                return symbol.match(new State(symbol, state, tokenizer.getCursor()), continuation);
            });
        }
        Symbol countDec = count(symbol, num - 1);
        return count.setMatch((state, continuation) -> {
            if (state.hasLeftRecursion(count, tokenizer.getCursor())) {
                return false;
            }
            return symbol.match(new State(symbol, state, tokenizer.getCursor()), s2 -> {
                return countDec.match(new State(symbol, state, tokenizer.getCursor()), continuation);
            });
        });
    }

    protected Symbol assign(String name, Symbol symbol) {
        Symbol assign = new Symbol("assign(" + name + "," + symbol + ")");
        return assign.setMatch((state, continuation) -> {
            if (state.hasLeftRecursion(assign, tokenizer.getCursor())) {
                return false;
            }
            int begin = tokenizer.getCursor();
            return symbol.match(new State(symbol, state, tokenizer.getCursor()), s2 -> {
                int end = tokenizer.getCursor();
                Objects.requireNonNull(state.getParent(), "Prohibited: setting variables on top-level symbols.").set(name, tokenizer.substring(begin, end));
                return continuation.test(s2);
            });
        });
    }
}
