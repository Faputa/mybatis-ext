package io.github.mybatisext.jpa;

@FunctionalInterface
public interface Match {

    boolean test(State state, Continuation continuation);
}
