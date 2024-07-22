package io.github.mybatisext.jpa;

import java.util.Objects;

public abstract class BaseParser<T extends Tokenizer> {

    protected final T tokenizer;

    protected BaseParser(T tokenizer) {
        this.tokenizer = tokenizer;
    }

    protected Symbol choice(Symbol... symbols) {
        return new Symbol() {
            @Override
            public boolean match(State state) {
                if (state.hasLeftRecursion(this)) {
                    return false;
                }
                int cursor = tokenizer.getCursor();
                for (Symbol symbol : symbols) {
                    if (symbol.match(new State(symbol, state, tokenizer.getCursor()))) {
                        return true;
                    }
                }
                tokenizer.setCursor(cursor);
                return false;
            }
        };
    }

    protected Symbol join(Symbol... symbols) {
        return new Symbol() {
            @Override
            public boolean match(State state) {
                if (state.hasLeftRecursion(this)) {
                    return false;
                }
                int cursor = tokenizer.getCursor();
                for (Symbol symbol : symbols) {
                    if (!symbol.match(new State(symbol, state, tokenizer.getCursor()))) {
                        tokenizer.setCursor(cursor);
                        return false;
                    }
                }
                return true;
            }
        };
    }

    protected Symbol optional(Symbol symbol) {
        return new Symbol() {
            @Override
            public boolean match(State state) {
                if (state.hasLeftRecursion(this)) {
                    return false;
                }
                symbol.match(new State(symbol, state, tokenizer.getCursor()));
                return true;
            }
        };
    }

    protected Symbol star(Symbol symbol) {
        return new Symbol() {
            @Override
            public boolean match(State state) {
                if (state.hasLeftRecursion(this)) {
                    return false;
                }
                while (symbol.match(new State(symbol, state, tokenizer.getCursor()))) {
                }
                return true;
            }
        };
    }

    protected Symbol plus(Symbol symbol) {
        return new Symbol() {
            @Override
            public boolean match(State state) {
                if (state.hasLeftRecursion(this)) {
                    return false;
                }
                int cursor = tokenizer.getCursor();
                int i = 0;
                while (symbol.match(new State(symbol, state, tokenizer.getCursor()))) {
                    i++;
                }
                if (i >= 1) {
                    return true;
                }
                tokenizer.setCursor(cursor);
                return false;
            }
        };
    }

    protected Symbol count(Symbol symbol, int num) {
        return new Symbol() {
            @Override
            public boolean match(State state) {
                if (state.hasLeftRecursion(this)) {
                    return false;
                }
                int cursor = tokenizer.getCursor();
                int i = 0;
                while (symbol.match(new State(symbol, state, tokenizer.getCursor()))) {
                    i++;
                    if (i == num) {
                        return true;
                    }
                }
                tokenizer.setCursor(cursor);
                return false;
            }
        };
    }

    protected Symbol assign(String name, Symbol symbol) {
        return new Symbol() {
            @Override
            public boolean match(State state) {
                if (state.hasLeftRecursion(this)) {
                    return false;
                }
                int begin = tokenizer.getCursor();
                boolean match = symbol.match(new State(symbol, state, tokenizer.getCursor()));
                int end = tokenizer.getCursor();
                Objects.requireNonNull(state.getParent(), "Prohibited: setting variables on top-level symbols.").set(name, tokenizer.substring(begin, end));
                return match;
            }
        };
    }
}
