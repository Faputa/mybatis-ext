package io.github.mybatisext.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

import io.github.mybatisext.model.JavaAnnotation;
import io.github.mybatisext.model.JavaParameter;

public class GenericParameter implements JavaParameter {

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

    @Override
    public JavaAnnotation annotation(Class<? extends Annotation> annotationType) {
        return ReflectionAnnotations.get(parameter, annotationType);
    }

    @Override
    public JavaAnnotation[] annotations(Class<? extends Annotation> annotationType) {
        return ReflectionAnnotations.getAll(parameter, annotationType);
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return parameter.getAnnotation(annotationClass);
    }

    @Override
    public String toString() {
        return parameter.toString();
    }
}
