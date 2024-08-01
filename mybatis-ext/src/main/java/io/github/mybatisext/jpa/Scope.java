package io.github.mybatisext.jpa;

public class Scope {

    private final Symbol owner;
    private Object returnValue;

    public Scope(Symbol owner) {
        this.owner = owner;
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
