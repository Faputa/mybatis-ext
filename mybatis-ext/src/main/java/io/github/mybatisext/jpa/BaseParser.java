package io.github.mybatisext.jpa;

import java.util.Objects;

public abstract class BaseParser<T extends Tokenizer> {

    protected final T tokenizer;

    protected BaseParser(T tokenizer) {
        this.tokenizer = tokenizer;
    }

    protected Symbol choice(Symbol... symbols) {
        Symbol choice = new Symbol("choice");
        return choice.setMatch(state -> {
            if (state.hasLeftRecursion(choice, tokenizer.getCursor())) {
                return false;
            }
            int cursor = tokenizer.getCursor();
            for (Symbol symbol : symbols) {
                if (symbol.match(new State(symbol, state, tokenizer.getCursor()))) {
                    return true;
                }
                tokenizer.setCursor(cursor);
            }
            return false;
        });
    }

    protected Symbol join(Symbol... symbols) {
        Symbol join = new Symbol("join");
        return join.setMatch(state -> {
            if (state.hasLeftRecursion(join, tokenizer.getCursor())) {
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
        });
    }

    protected Symbol optional(Symbol symbol) {
        Symbol optional = new Symbol("optional");
        return optional.setMatch(state -> {
            if (state.hasLeftRecursion(optional, tokenizer.getCursor())) {
                return false;
            }
            symbol.match(new State(symbol, state, tokenizer.getCursor()));
            return true;
        });
    }

    protected Symbol star(Symbol symbol) {
        Symbol star = new Symbol("star");
        return star.setMatch(state -> {
            if (state.hasLeftRecursion(star, tokenizer.getCursor())) {
                return false;
            }
            while (symbol.match(new State(symbol, state, tokenizer.getCursor()))) {
            }
            return true;
        });
    }

    protected Symbol plus(Symbol symbol) {
        Symbol plus = new Symbol("plus");
        return plus.setMatch(state -> {
            if (state.hasLeftRecursion(plus, tokenizer.getCursor())) {
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
        });
    }

    protected Symbol count(Symbol symbol, int num) {
        Symbol count = new Symbol("count");
        return count.setMatch(state -> {
            if (state.hasLeftRecursion(count, tokenizer.getCursor())) {
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
        });
    }

    protected Symbol assign(String name, Symbol symbol) {
        Symbol assign = new Symbol("assign");
        return assign.setMatch(state -> {
            if (state.hasLeftRecursion(assign, tokenizer.getCursor())) {
                return false;
            }
            int begin = tokenizer.getCursor();
            boolean match = symbol.match(new State(symbol, state, tokenizer.getCursor()));
            int end = tokenizer.getCursor();
            Objects.requireNonNull(state.getParent(), "Prohibited: setting variables on top-level symbols.").set(name, tokenizer.substring(begin, end));
            return match;
        });
    }
}
