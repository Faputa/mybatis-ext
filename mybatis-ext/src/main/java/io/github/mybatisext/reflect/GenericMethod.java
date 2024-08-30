package io.github.mybatisext.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

public class GenericMethod {

    private final Method method;
    private final Class<?> actualClass;
    private final Type[] actualTypeArguments;

    public GenericMethod(Method method, Class<?> actualClass, Type[] actualTypeArguments) {
        this.method = method;
        this.actualClass = actualClass;
        this.actualTypeArguments = actualTypeArguments;
    }

    public Method getMethod() {
        return method;
    }

    public GenericParameter[] getParameters() {
        Parameter[] parameters = method.getParameters();
        Type[] types = method.getGenericParameterTypes();
        GenericParameter[] genericParameters = new GenericParameter[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            genericParameters[i] = new GenericParameter(parameters[i], GenericTypeFactory.build(types[i], actualClass, actualTypeArguments));
        }
        return genericParameters;
    }

    public GenericType getGenericReturnType() {
        return GenericTypeFactory.build(method.getGenericReturnType(), actualClass, actualTypeArguments);
    }

    public GenericType[] getGenericParameterTypes() {
        Type[] types = method.getGenericParameterTypes();
        GenericType[] genericTypes = new GenericType[types.length];
        for (int i = 0; i < types.length; i++) {
            genericTypes[i] = GenericTypeFactory.build(types[i], actualClass, actualTypeArguments);
        }
        return genericTypes;
    }

    public String getName() {
        return method.getName();
    }

    @Override
    public String toString() {
        return method.toString();
    }
}
