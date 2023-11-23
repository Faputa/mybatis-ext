package com.mybatisext.util;

import java.lang.annotation.Annotation;

import javax.annotation.Nullable;

public class AnnotationResolver {

    public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        if (clazz.isAnnotationPresent(annotation)) {
            return true;
        }
        if (clazz.getSuperclass() != null && hasAnnotation(clazz.getSuperclass(), annotation)) {
            return true;
        }
        for (Class<?> interf : clazz.getInterfaces()) {
            if (hasAnnotation(interf, annotation)) {
                return true;
            }
        }
        return false;
    }

    public static <T extends Annotation> T findAnnotation(Class<?> clazz, Class<T> annotationClass,
            @Nullable Class<?> fixedClass) {
        // 检查固定类是否在继承路径上
        if (fixedClass != null && !clazz.isAssignableFrom(fixedClass) && !fixedClass.isAssignableFrom(clazz)) {
            return null;
        }
        T annotation = clazz.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            T superClassAnnotation = findAnnotation(superClass, annotationClass, fixedClass);
            if (superClassAnnotation != null) {
                return superClassAnnotation;
            }
        }
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> iface : interfaces) {
            T interfaceAnnotation = findAnnotation(iface, annotationClass, fixedClass);
            if (interfaceAnnotation != null) {
                return interfaceAnnotation;
            }
        }
        return null;
    }

}
