package io.github.mybatisext.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.model.JavaAnnotation;

public final class ReflectionAnnotations {

    private ReflectionAnnotations() {
    }

    public static JavaAnnotation get(AnnotatedElement element, Class<? extends Annotation> type) {
        return of(element.getAnnotation(type));
    }

    public static JavaAnnotation[] getAll(AnnotatedElement element, Class<? extends Annotation> type) {
        return Arrays.stream(element.getAnnotationsByType(type)).map(ReflectionAnnotations::of).toArray(JavaAnnotation[]::new);
    }

    public static JavaAnnotation of(Annotation annotation) {
        if (annotation == null) {
            return null;
        }
        return new JavaAnnotation() {

            @Override
            @SuppressWarnings("unchecked")
            public <T> T value(String name) {
                try {
                    Object value = annotation.annotationType().getMethod(name).invoke(annotation);
                    if (value instanceof Class) {
                        value = GenericTypeFactory.build((Class<?>) value);
                    } else if (value instanceof Annotation[]) {
                        value = Arrays.stream((Annotation[]) value).map(ReflectionAnnotations::of).toArray(JavaAnnotation[]::new);
                    }
                    return (T) value;
                } catch (ReflectiveOperationException e) {
                    throw new MybatisExtException(e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e);
                }
            }
        };
    }
}
