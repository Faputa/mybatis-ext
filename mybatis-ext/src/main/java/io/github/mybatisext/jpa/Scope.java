package io.github.mybatisext.jpa;

public class Scope<T extends Tokenizer> {

    private final String name;
    private final State<T> outside;

    public Scope(String name, State<T> outside) {
        this.name = name;
        this.outside = outside;
    }

    public State<T> getOutside() {
        return outside;
    }

    @Override
    public String toString() {
        return name;
    }
}
