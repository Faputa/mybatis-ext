package io.github.mybatisext.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class GenericField {

    private final Field field;
    private final GenericType genericType;

    public GenericField(Field field, Class<?> actualClass, Type[] actualTypeArguments) {
        this.field = field;
        this.genericType = GenericTypeFactory.build(field.getGenericType(), actualClass, actualTypeArguments);
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

    @Override
    public String toString() {
        return field.toString();
    }
}
