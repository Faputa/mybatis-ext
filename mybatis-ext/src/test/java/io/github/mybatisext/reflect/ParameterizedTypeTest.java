package io.github.mybatisext.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class ParameterizedTypeTest {

    @Test
    void comparesParameterizedTypesByAllArguments() throws Exception {
        GenericType strings = build("strings");
        GenericType stringsCopy = build("stringsCopy");
        GenericType integers = build("integers");

        assertEquals(strings, stringsCopy);
        assertEquals(strings.hashCode(), stringsCopy.hashCode());
        assertSame(strings, stringsCopy);
        assertNotEquals(strings, integers);
    }

    @Test
    void preservesNestedParameterizedArguments() throws Exception {
        GenericType nested = build("nested");

        assertEquals(Map.class, nested.getType());
        assertEquals(String.class, nested.getTypeParameters()[0].getType());
        assertEquals(List.class, nested.getTypeParameters()[1].getType());
        assertEquals(Integer.class, nested.getTypeParameters()[1].getTypeParameters()[0].getType());
    }

    @Test
    void resolvesArraysWithParameterizedComponents() throws Exception {
        GenericType array = build("arrays");

        assertTrue(array.isArray());
        assertEquals(List.class, array.getComponentType().getType());
        assertEquals(String.class, array.getComponentType().getTypeParameters()[0].getType());
    }

    private GenericType build(String fieldName) throws Exception {
        Type type = ParameterizedFields.class.getDeclaredField(fieldName).getGenericType();
        return GenericTypeFactory.build(type);
    }

    static class ParameterizedFields {
        private List<String> strings;
        private List<String> stringsCopy;
        private List<Integer> integers;
        private Map<String, List<Integer>> nested;
        private List<String>[] arrays;
    }
}
