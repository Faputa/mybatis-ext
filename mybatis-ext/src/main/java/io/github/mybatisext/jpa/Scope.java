package io.github.mybatisext.jpa;

public class Scope {

    private final String name;
    private Object returnValue;

    public Scope(String name) {
        this.name = name;
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
