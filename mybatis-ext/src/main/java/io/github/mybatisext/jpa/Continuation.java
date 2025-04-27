package io.github.mybatisext.jpa;

@FunctionalInterface
public interface Continuation<T extends Tokenizer> {

    boolean test(State<T> state);
}
