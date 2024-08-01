package io.github.mybatisext.jpa;

@FunctionalInterface
public interface Continuation {

    // result是上一个返回值
    boolean test(State state, Object result);
}
