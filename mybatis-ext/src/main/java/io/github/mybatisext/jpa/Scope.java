package io.github.mybatisext.jpa;

public class Scope {

    private final String name;
    private final State outside;

    public Scope(String name, State outside) {
        this.name = name;
        this.outside = outside;
    }

    public State getOutside() {
        return outside;
    }

    @Override
    public String toString() {
        return name;
    }
}
