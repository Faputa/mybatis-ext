package io.github.mybatisext.jpa;

@FunctionalInterface
public interface Match {

    // result是默认返回值
    boolean test(State state, Object result, Continuation continuation);
}
