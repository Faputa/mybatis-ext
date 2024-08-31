package io.github.mybatisext.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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
            genericParameters[i] = new GenericParameter(parameters[i], GenericTypeFactory.build(types[i], typeMap));
        }
        return genericParameters;
    }

    public GenericType getGenericReturnType() {
        return GenericTypeFactory.build(method.getGenericReturnType(), typeMap);
    }

    public GenericType[] getGenericParameterTypes() {
        Type[] types = method.getGenericParameterTypes();
        GenericType[] genericTypes = new GenericType[types.length];
        for (int i = 0; i < types.length; i++) {
            genericTypes[i] = GenericTypeFactory.build(types[i], typeMap);
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
