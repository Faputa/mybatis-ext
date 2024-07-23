package io.github.mybatisext.jpa;

@FunctionalInterface
public interface Symbol {

    boolean match(State state);

    default Symbol getSymbol() {
        return this;
    }
}
