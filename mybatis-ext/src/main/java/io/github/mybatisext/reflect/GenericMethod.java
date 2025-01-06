package io.github.mybatisext.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

public class GenericMethod {

    private final Method method;
    private final Map<TypeVariable<?>, Type> typeMap;

    public GenericMethod(Method method, Map<TypeVariable<?>, Type> typeMap) {
        this.method = method;
        this.typeMap = typeMap;
    }

    public Method getMethod() {
        return method;
    }

    public GenericParameter[] getParameters() {
        Parameter[] parameters = method.getParameters();
        Type[] types = method.getGenericParameterTypes();
        GenericParameter[] genericParameters = new GenericParameter[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            genericParameters[i] = new GenericParameter(parameters[i], GenericTypeFactory.build(types[i], new HashMap<>(typeMap)));
        }
        return genericParameters;
    }

    public GenericType getGenericReturnType() {
        return GenericTypeFactory.build(method.getGenericReturnType(), new HashMap<>(typeMap));
    }

    public GenericType[] getGenericParameterTypes() {
        Type[] types = method.getGenericParameterTypes();
        GenericType[] genericTypes = new GenericType[types.length];
        for (int i = 0; i < types.length; i++) {
            genericTypes[i] = GenericTypeFactory.build(types[i], new HashMap<>(typeMap));
        }
        return genericTypes;
    }

    public String getName() {
        return method.getName();
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return method.isAnnotationPresent(annotationClass);
    }


    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return method.getAnnotation(annotationClass);
    }

    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return method.getAnnotationsByType(annotationClass);
    }

    public Class<?> getDeclaringClass() {
        return method.getDeclaringClass();
    }

    public boolean isBridge() {
        return method.isBridge();
    }

    public boolean isDefault() {
        return method.isDefault();
    }

    @Override
    public String toString() {
        return method.toString();
    }
}
