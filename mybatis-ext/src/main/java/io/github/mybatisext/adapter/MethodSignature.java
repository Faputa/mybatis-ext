package io.github.mybatisext.adapter;

import java.lang.reflect.Method;
import java.util.Objects;

public class MethodSignature {

    private final String methodName;
    private final Class<?>[] parameterTypes;

    public MethodSignature(String methodName, Class<?>[] parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }

    public boolean match(Method method) {
        return Objects.equals(methodName, method.getName()) && Objects.deepEquals(parameterTypes, method.getParameterTypes());
    }
}
