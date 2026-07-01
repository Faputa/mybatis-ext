package io.github.mybatisext.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.ibatis.type.JdbcType;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.util.Getter;
import io.github.mybatisext.util.StringUtils;
import io.github.mybatisext.util.TypeUtils;

public class Variable implements Getter<Variable> {

    private final String name;
    private final String fullName;
    private final GenericType javaType;
    private final Map<String, Variable> nameToVariable = new HashMap<>();
    // 延迟绑定
    private JdbcType jdbcType;

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

    public JdbcType getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(JdbcType jdbcType) {
        this.jdbcType = jdbcType;
    }

    public String getPlaceholder() {
        if (jdbcType != null) {
            return "#{" + getFullName() + ", jdbcType=" + jdbcType + "}";
        }
        return "#{" + getFullName() + "}";
    }

    public String getBindName() {
        return "__" + name + "__bind";
    }

    public String getBindPlaceholder() {
        if (jdbcType != null) {
            return "#{" + getBindName() + ", jdbcType=" + jdbcType + "}";
        }
        return "#{" + getBindName() + "}";
    }

    public String getItemName() {
        return "__" + name + "__item";
    }

    public String getItemPlaceholder() {
        if (jdbcType != null) {
            return "#{" + getItemName() + ", jdbcType=" + jdbcType + "}";
        }
        return "#{" + getItemName() + "}";
    }

    public Variable getItemVariable() {
        Variable variable = new Variable(getItemName(), TypeUtils.unwrapToGenericType(javaType));
        variable.setJdbcType(jdbcType);
        return variable;
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
