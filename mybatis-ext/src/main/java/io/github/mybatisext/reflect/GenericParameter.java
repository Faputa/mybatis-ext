package io.github.mybatisext.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

public class GenericParameter {

    private final Parameter parameter;
    private final GenericType genericType;

    public GenericParameter(Parameter parameter, GenericType genericType) {
        this.parameter = parameter;
        this.genericType = genericType;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public GenericType getGenericType() {
        return genericType;
    }

    public Class<?> getType() {
        return genericType.getType();
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return parameter.getAnnotation(annotationClass);
    }

    @Override
    public String toString() {
        return parameter.toString();
    }
}
