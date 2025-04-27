package io.github.mybatisext.jpa;

public class Scope<T extends Tokenizer> {

    private final Symbol<T> owner;
    private final State<T> outside;

    public Scope(Symbol<T> owner, State<T> outside) {
        this.owner = owner;
        this.outside = outside;
    }

    public State<T> getOutside() {
        return outside;
    }

    @Override
    public String toString() {
        return String.valueOf(owner);
    }
}
