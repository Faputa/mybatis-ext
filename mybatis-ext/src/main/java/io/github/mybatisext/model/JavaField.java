package io.github.mybatisext.model;

public interface JavaField extends JavaAnnotatedElement {

    String getName();

    JavaType getGenericType();
}
