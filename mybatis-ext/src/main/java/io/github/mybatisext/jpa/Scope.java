package io.github.mybatisext.jpa;

public class Scope {

    private final Symbol owner;
    private final State outside;
    // TODO returnValue应该定义在state中
    private Object returnValue;

    public Scope(Symbol owner, State guard) {
        this.owner = owner;
        this.outside = guard;
    }

    public State getOutside() {
        return outside;
    }

    @Override
    public String toString() {
        return String.valueOf(owner);
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }
}
