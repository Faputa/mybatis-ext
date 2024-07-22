package io.github.mybatisext.jpa;

// 允许构造自引用的非终结符
public class Nonterminal implements Symbol {

    private Symbol symbol;

    @Override
    public boolean match(State state) {
        return symbol.match(state);
    }

    public void set(Symbol symbol) {
        this.symbol = symbol;
    }
}
