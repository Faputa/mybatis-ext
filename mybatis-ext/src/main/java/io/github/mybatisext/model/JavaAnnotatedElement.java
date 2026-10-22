package io.github.mybatisext.model;

import java.lang.annotation.Annotation;

public interface JavaAnnotatedElement {

    JavaAnnotation annotation(Class<? extends Annotation> annotationType);

    JavaAnnotation[] annotations(Class<? extends Annotation> annotationType);

    default boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        return annotation(annotationType) != null;
    }
}
