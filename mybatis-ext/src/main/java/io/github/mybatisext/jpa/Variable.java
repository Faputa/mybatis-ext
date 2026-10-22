package io.github.mybatisext.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.ibatis.type.JdbcType;

import io.github.mybatisext.model.JavaAnnotatedElement;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.JavaTypes;
import io.github.mybatisext.util.Getter;
import io.github.mybatisext.util.StringUtils;

public class Variable implements Getter<Variable> {

    private final String name;
    private final String fullName;
    private final JavaType javaType;
    private final JavaAnnotatedElement source;
    private final Map<String, Variable> nameToVariable = new HashMap<>();
    // 延迟绑定
    private JdbcType jdbcType;

    public Variable(String name, JavaType javaType) {
        this("", name, javaType);
    }

    public Variable(String prefix, String name, JavaType javaType) {
        this(prefix, name, javaType, null);
    }

    public Variable(String prefix, String name, JavaType javaType, JavaAnnotatedElement source) {
        this.name = name;
        this.fullName = StringUtils.isNotBlank(prefix) ? prefix + "." + name : name;
        this.javaType = javaType;
        this.source = source;
    }

    Variable(Variable source) {
        this.name = source.name;
        this.fullName = source.fullName;
        this.javaType = source.javaType;
        this.source = source.source;
        this.jdbcType = source.jdbcType;
        this.nameToVariable.putAll(source.nameToVariable);
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public JavaType getJavaType() {
        return javaType;
    }

    /** 变量实际绑定的参数、字段或 getter，不参与变量相等性和 SQL 渲染。 */
    public JavaAnnotatedElement getSource() {
        return source;
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
        Variable variable = new Variable("", getItemName(), JavaTypes.unwrap(javaType), source);
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
