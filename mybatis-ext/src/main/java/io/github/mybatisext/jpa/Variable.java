package io.github.mybatisext.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.util.Getter;
import io.github.mybatisext.util.StringUtils;

public class Variable implements Getter<Variable> {

    private final String name;
    private final String fullName;
    private final GenericType javaType;
    private final Map<String, Variable> nameToVariable = new HashMap<>();

    public Variable(String name, GenericType javaType) {
        this("", name, javaType);
    }

    public Variable(String prefix, String name, GenericType javaType) {
        this.name = name;
        this.fullName = StringUtils.isNotBlank(prefix) ? prefix + "." + name : name;
        this.javaType = javaType;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public GenericType getJavaType() {
        return javaType;
    }

    public Map<String, Variable> getNameToVariable() {
        return nameToVariable;
    }

    @Override
    public Variable get(String key) {
        return nameToVariable.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Variable variable = (Variable) o;
        return Objects.equals(name, variable.name) && Objects.equals(fullName, variable.fullName) && Objects.equals(javaType, variable.javaType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fullName, javaType);
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
