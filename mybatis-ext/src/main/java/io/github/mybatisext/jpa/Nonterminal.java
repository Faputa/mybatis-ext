package io.github.mybatisext.jpa;

import java.util.function.Function;

// 允许构造自引用的非终结符
public class Nonterminal implements Symbol {

    private final String name;
    private Symbol symbol;

    public Nonterminal(String name) {
        this.name = name;
    }

    @Override
    public boolean match(State state) {
        return symbol.match(state);
    }

    @Override
    public Symbol getSymbol() {
        return symbol;
    }

    public Nonterminal set(Symbol symbol) {
        this.symbol = symbol;
        return this;
    }

    public Nonterminal setF(Function<Symbol, Symbol> function) {
        this.symbol = function.apply(this);
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}
