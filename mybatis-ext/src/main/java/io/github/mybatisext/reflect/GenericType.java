package io.github.mybatisext.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GenericType implements Type {

    private final Class<?> type;
    private final Map<TypeVariable<?>, Type> typeMap;

    public GenericType(Class<?> type, Map<TypeVariable<?>, Type> typeMap) {
        this.type = type;
        this.typeMap = typeMap;
    }

    public Class<?> getType() {
        return type;
    }

    public GenericType[] getTypeParameters() {
        TypeVariable<?>[] typeVariables = type.getTypeParameters();
        GenericType[] genericTypes = new GenericType[typeVariables.length];
        for (int i = 0; i < typeVariables.length; i++) {
            genericTypes[i] = GenericTypeFactory.build(typeVariables[i], typeMap);
        }
        return genericTypes;
    }

    public GenericField[] getDeclaredFields() {
        Field[] fields = type.getDeclaredFields();
        GenericField[] genericFields = new GenericField[fields.length];
        for (int i = 0; i < fields.length; i++) {
            genericFields[i] = new GenericField(fields[i], typeMap);
        }
        return genericFields;
    }

    public GenericMethod[] getDeclaredMethods() {
        Method[] methods = type.getDeclaredMethods();
        GenericMethod[] genericMethods = new GenericMethod[methods.length];
        for (int i = 0; i < methods.length; i++) {
            genericMethods[i] = new GenericMethod(methods[i], typeMap);
        }
        return genericMethods;
    }

    public GenericField[] getFields() {
        Set<Field> fields = Arrays.stream(type.getFields()).collect(Collectors.toSet());
        GenericField[] genericFields = new GenericField[fields.size()];
        int count = 0;
        List<GenericType> queue = new ArrayList<>();
        queue.add(this);
        for (int i = 0; i < queue.size(); i++) {
            GenericType genericType = queue.get(i);
            for (GenericField genericField : genericType.getDeclaredFields()) {
                if (fields.contains(genericField.getField())) {
                    genericFields[count++] = genericField;
                }
            }
            queue.addAll(Arrays.asList(genericType.getGenericInterfaces()));
            GenericType genericSuperclass = genericType.getGenericSuperclass();
            if (genericSuperclass != null) {
                queue.add(genericSuperclass);
            }
        }
        return genericFields;
    }

    public GenericMethod[] getMethods() {
        Set<Method> methods = Arrays.stream(type.getMethods()).collect(Collectors.toSet());
        GenericMethod[] genericMethods = new GenericMethod[methods.size()];
        int count = 0;
        List<GenericType> queue = new ArrayList<>();
        queue.add(this);
        for (int i = 0; i < queue.size(); i++) {
            GenericType genericType = queue.get(i);
            for (GenericMethod genericMethod : genericType.getDeclaredMethods()) {
                if (methods.contains(genericMethod.getMethod())) {
                    genericMethods[count++] = genericMethod;
                }
            }
            queue.addAll(Arrays.asList(genericType.getGenericInterfaces()));
            GenericType genericSuperclass = genericType.getGenericSuperclass();
            if (genericSuperclass != null) {
                queue.add(genericSuperclass);
            }
        }
        return genericMethods;
    }

    public GenericType getGenericSuperclass() {
        Type superType = type.getGenericSuperclass();
        if (superType == null) {
            return null;
        }
        return GenericTypeFactory.build(superType, typeMap);
    }

    public GenericType[] getGenericInterfaces() {
        Type[] interfaces = type.getGenericInterfaces();
        GenericType[] types = new GenericType[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            types[i] = GenericTypeFactory.build(interfaces[i], typeMap);
        }
        return types;
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
