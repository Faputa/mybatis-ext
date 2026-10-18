package io.github.mybatisext.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.model.JavaTypes;

public class GenericTypeFactoryTest {

    interface TestMap extends Map<String, List<Integer>> {
    }

    @Test
    void resolvesNestedGenericInterfaceArguments() {
        GenericType genericType = GenericTypeFactory.build(TestMap.class);
        assertEquals(List.class, genericType.getGenericInterfaces()[0].getTypeParameters()[1].getType());
        assertEquals(Integer.class, genericType.getGenericInterfaces()[0].getTypeParameters()[1].getTypeParameters()[0].getType());
        assertEquals(genericType.getGenericInterfaces()[0].getTypeParameters()[1], JavaTypes.typeArgument(genericType, Map.class, 1));
    }

    @Test
    void preservesInheritedCollectionElementTypes() {
        GenericType list = GenericTypeFactory.build(StringList.class);

        assertTrue(list.isAssignableTo(List.class));
        assertEquals(GenericTypeFactory.build(String.class), JavaTypes.unwrap(list));
    }

    @Test
    void comparesErasedTypesWithoutChangingGenericArguments() throws Exception {
        GenericType strings = GenericTypeFactory.build(ParameterizedFields.class.getDeclaredField("first").getGenericType());
        GenericType numbers = GenericTypeFactory.build(ParameterizedFields.class.getDeclaredField("numbers").getGenericType());

        assertTrue(strings.isAssignableFrom(numbers));
        assertTrue(strings.is(List.class));
        assertTrue(numbers.is(List.class));
        assertFalse(strings.is(ArrayList.class));
        assertEquals(String.class, strings.getTypeParameters()[0].getType());
        assertEquals(Integer.class, numbers.getTypeParameters()[0].getType());
    }

    @Test
    void supportsArrayCovarianceWithoutPrimitiveConversions() {
        assertTrue(GenericTypeFactory.build(String[].class).isAssignableTo(Object[].class));
        assertTrue(GenericTypeFactory.build(String[].class).isAssignableTo(CharSequence[].class));
        assertFalse(GenericTypeFactory.build(String[].class).isAssignableTo(Number[].class));
        assertTrue(GenericTypeFactory.build(int[][].class).isAssignableTo(Object[].class));
        assertFalse(GenericTypeFactory.build(int[].class).isAssignableTo(Object[].class));
        assertFalse(GenericTypeFactory.build(int[].class).isAssignableTo(long[].class));
        assertFalse(GenericTypeFactory.build(int.class).isAssignableTo(Integer.class));
        assertFalse(GenericTypeFactory.build(int.class).isAssignableTo(long.class));
    }

    @Test
    void resolvesRuntimeClassesForGenericArrays() throws Exception {
        GenericType holder = GenericTypeFactory.build(ParameterizedFields.class.getDeclaredField("holder").getGenericType());
        GenericType array = findField(holder, "array").getGenericType();
        GenericType matrix = findField(holder, "matrix").getGenericType();

        assertTrue(array.isAssignableTo(Long[].class));
        assertTrue(array.isAssignableTo(Number[].class));
        assertFalse(array.isAssignableTo(Integer[].class));
        assertTrue(matrix.isAssignableTo(Long[][].class));
        assertTrue(matrix.isAssignableTo(Object[].class));
    }

    @Test
    void cachesTypesAndPassesThroughGenericTypeInstances() throws Exception {
        Type first = ParameterizedFields.class.getDeclaredField("first").getGenericType();
        Type second = ParameterizedFields.class.getDeclaredField("second").getGenericType();
        GenericType genericType = GenericTypeFactory.build(first);

        assertSame(genericType, GenericTypeFactory.build(first));
        assertSame(genericType, GenericTypeFactory.build(second));
        assertSame(genericType, GenericTypeFactory.build(genericType, new HashMap<>()));
    }

    @Test
    void resolvesExplicitTypeVariableMappings() {
        TypeVariable<?> variable = GenericHolder.class.getTypeParameters()[0];
        Map<TypeVariable<?>, Type> typeMap = new HashMap<>();
        typeMap.put(variable, Long.class);

        assertEquals(Long.class, GenericTypeFactory.build(variable, typeMap).getType());
    }

    @Test
    void resolvesFieldsFromDirectParameterizedTypes() throws Exception {
        Field holder = ParameterizedFields.class.getDeclaredField("holder");
        GenericType holderType = GenericTypeFactory.build(holder.getGenericType());

        GenericField value = findField(holderType, "value");
        GenericField values = findField(holderType, "values");
        assertEquals(Long.class, value.getType());
        assertEquals(List.class, values.getType());
        assertEquals(Long.class, values.getGenericType().getTypeParameters()[0].getType());
    }

    @Test
    void rejectsUnresolvedTypeVariablesWithClearMessage() {
        TypeVariable<?> variable = GenericHolder.class.getTypeParameters()[0];

        MybatisExtException exception = assertThrows(MybatisExtException.class, () -> GenericTypeFactory.build(variable, new HashMap<>()));

        assertTrue(exception.getMessage().contains("Unresolved type variable 'T'"));
    }

    private GenericField findField(GenericType type, String name) {
        for (GenericField field : type.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        throw new AssertionError(name);
    }

    static class GenericHolder<T> {

        private T value;
        private List<T> values;
        private T[] array;
        private T[][] matrix;
    }

    static class StringList extends ArrayList<String> {
    }

    static class ParameterizedFields {

        private List<String> first;
        private List<String> second;
        private List<Integer> numbers;
        private GenericHolder<Long> holder;
    }
}
