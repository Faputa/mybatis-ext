package io.github.mybatisext.jpa;

@FunctionalInterface
public interface Match<T extends Tokenizer> {

    boolean test(State<T> state, Continuation<T> continuation);
}
