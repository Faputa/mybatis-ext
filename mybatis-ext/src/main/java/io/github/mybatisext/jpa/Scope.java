package io.github.mybatisext.jpa;

public class Scope {

    private final Symbol owner;
    private final State outside;

    public Scope(Symbol owner, State outside) {
        this.owner = owner;
        this.outside = outside;
    }

    public State getOutside() {
        return outside;
    }

    @Override
    public String toString() {
        return String.valueOf(owner);
    }
}
