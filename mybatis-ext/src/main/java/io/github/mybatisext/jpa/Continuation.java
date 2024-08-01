package io.github.mybatisext.jpa;

@FunctionalInterface
public interface Continuation {

    boolean test(State state, Object result);
}
