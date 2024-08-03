package io.github.mybatisext.jpa;

public class Scope {

    private final String name;
    private final State guard;
    // TODO returnValue应该定义在state中
    private Object returnValue;

    public Scope(String name, State guard) {
        this.name = name;
        this.guard = guard;
    }

    public State getGuard() {
        return guard;
    }

    @Override
    public String toString() {
        return name;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }
}
