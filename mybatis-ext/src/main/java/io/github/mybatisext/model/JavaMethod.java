package io.github.mybatisext.model;

public interface JavaMethod extends JavaAnnotatedElement {

    String getName();

    JavaType getDeclaringType();

    JavaType getGenericReturnType();

    JavaParameter[] getParameters();

    boolean isBridge();

    boolean isDefault();
}
