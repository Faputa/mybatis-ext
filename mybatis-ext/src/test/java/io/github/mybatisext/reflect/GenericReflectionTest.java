package io.github.mybatisext.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mybatisext.exception.MybatisExtException;

class GenericReflectionTest {

    @Test
    void resolvesInheritedFieldsMethodsParametersAndGenericArrays() {
        GenericType parentType = GenericTypeFactory.build(StringChild.class).getGenericSuperclass();

        GenericField value = field(parentType, "value");
        GenericField values = field(parentType, "values");
        GenericField array = field(parentType, "array");
        GenericMethod convert = method(parentType, "convert");

        assertEquals(String.class, value.getType());
        assertEquals(List.class, values.getType());
        assertEquals(String.class, values.getGenericType().getTypeParameters()[0].getType());
        assertTrue(array.getGenericType().isArray());
        assertEquals(String.class, array.getGenericType().getComponentType().getType());
        assertEquals(String.class, convert.getGenericReturnType().getType());
        assertEquals(String.class, convert.getParameters()[0].getType());
        assertEquals(String.class, convert.getGenericParameterTypes()[0].getType());
    }

    @Test
    void distinguishesGenericArraysByComponentType() {
        GenericType stringArray = field(GenericTypeFactory.build(StringChild.class).getGenericSuperclass(), "array").getGenericType();
        GenericType integerArray = field(GenericTypeFactory.build(IntegerChild.class).getGenericSuperclass(), "array").getGenericType();
        GenericType directStringArray = GenericTypeFactory.build(String[].class);

        assertNotEquals(stringArray, integerArray);
        assertNotEquals(stringArray.hashCode(), integerArray.hashCode());
        assertEquals(stringArray, directStringArray);
        assertTrue(directStringArray.isArray());
        assertEquals(String.class, directStringArray.getComponentType().getType());
        assertEquals("java.lang.String[]", stringArray.getName());
        assertEquals("String[]", stringArray.getSimpleName());
        assertEquals("class java.lang.String[]", stringArray.toString());
    }

    @Test
    void resolvesPublicInheritedMembersWithSubstitutedTypes() {
        GenericType childType = GenericTypeFactory.build(StringChild.class);

        GenericField publicValue = Arrays.stream(childType.getFields()).filter(field -> field.getName().equals("publicValue")).findFirst().orElseThrow(AssertionError::new);
        GenericMethod publicConvert = Arrays.stream(childType.getMethods()).filter(method -> method.getName().equals("publicConvert")).findFirst().orElseThrow(AssertionError::new);

        assertEquals(String.class, publicValue.getType());
        assertEquals(String.class, publicConvert.getGenericReturnType().getType());
        assertEquals(String.class, publicConvert.getParameters()[0].getType());
    }

    @Test
    void rejectsUnsupportedWildcardTypesWithClearMessage() {
        MybatisExtException exception = assertThrows(MybatisExtException.class, () -> GenericTypeFactory.build(WildcardHolder.class).getDeclaredFields());

        assertTrue(exception.getMessage().contains("Unsupported Type"));
        assertTrue(exception.getMessage().contains("WildcardType"));
    }

    private GenericField field(GenericType type, String name) {
        return Arrays.stream(type.getDeclaredFields()).filter(field -> field.getName().equals(name)).findFirst().orElseThrow(AssertionError::new);
    }

    private GenericMethod method(GenericType type, String name) {
        return Arrays.stream(type.getDeclaredMethods()).filter(method -> method.getName().equals(name)).findFirst().orElseThrow(AssertionError::new);
    }

    static class GenericParent<T> {
        private T value;
        private List<T> values;
        private T[] array;
        public T publicValue;

        T convert(T input) {
            return input;
        }

        public T publicConvert(T input) {
            return input;
        }
    }

    static class StringChild extends GenericParent<String> {
    }

    static class IntegerChild extends GenericParent<Integer> {
    }

    static class WildcardHolder {
        private List<? extends Number> values;
    }
}
