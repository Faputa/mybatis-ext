package io.github.mybatisext.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

public class GenericField {

    private final Field field;
    private final GenericType genericType;

    public GenericField(Field field, Map<TypeVariable<?>, Type> typeMap) {
        this.field = field;
        this.genericType = GenericTypeFactory.build(field.getGenericType(), new HashMap<>(typeMap));
    }

    public Field getField() {
        return field;
    }

    public GenericType getGenericType() {
        return genericType;
    }

    public Class<?> getType() {
        return genericType.getType();
    }

    public String getName() {
        return field.getName();
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return field.isAnnotationPresent(annotationClass);
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return field.getAnnotation(annotationClass);
    }

    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return field.getAnnotationsByType(annotationClass);
    }

    @Override
    public String toString() {
        return field.toString();
    }
}
