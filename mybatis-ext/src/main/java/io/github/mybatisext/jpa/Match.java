package io.github.mybatisext.jpa;

@FunctionalInterface
public interface Match {

    boolean test(State state, Tokenizer tokenizer, Continuation continuation);
}
