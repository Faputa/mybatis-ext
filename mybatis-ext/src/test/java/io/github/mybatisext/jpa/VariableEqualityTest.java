package io.github.mybatisext.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.util.TypeUtils;

class VariableEqualityTest {

    @Test
    void primitiveAndWrapperVariablesAreEqualWithoutReplacingTheirTypes() {
        for (Class<?> primitive : new Class<?>[]{boolean.class, byte.class, short.class, int.class, long.class, float.class, double.class, char.class}) {
            GenericType primitiveType = GenericTypeFactory.build(primitive);
            GenericType wrapperType = GenericTypeFactory.build(TypeUtils.wrapPrimitive(primitive));
            Variable first = new Variable("query", "value", primitiveType);
            Variable second = new Variable("query", "value", wrapperType);

            assertEquals(first, second);
            assertEquals(second, first);
            assertEquals(first.hashCode(), second.hashCode());
            assertSame(primitiveType, first.getJavaType());
            assertSame(wrapperType, second.getJavaType());
            assertSame(primitiveType, new Variable(first).getJavaType());

            Map<Variable, String> values = new HashMap<>();
            values.put(first, "primitive");
            values.put(second, "wrapper");
            assertEquals(1, values.size());
            assertEquals("wrapper", values.get(first));
        }
    }

    @Test
    void differentScalarTypesRemainDistinct() {
        assertNotEquals(new Variable("value", GenericTypeFactory.build(long.class)), new Variable("value", GenericTypeFactory.build(Integer.class)));
        assertNotEquals(new Variable("value", GenericTypeFactory.build(Long.class)), new Variable("value", GenericTypeFactory.build(String.class)));
    }

    @Test
    void variableNamesAndPrefixesStillParticipateInEquality() {
        Variable variable = new Variable("first", "value", GenericTypeFactory.build(long.class));
        assertNotEquals(variable, new Variable("second", "value", GenericTypeFactory.build(Long.class)));
        assertNotEquals(variable, new Variable("first", "other", GenericTypeFactory.build(Long.class)));
    }

    @Test
    void genericArgumentsAndArrayComponentTypesRemainDistinct() throws Exception {
        GenericType strings = GenericTypeFactory.build(Fields.class.getDeclaredField("strings").getGenericType());
        GenericType longs = GenericTypeFactory.build(Fields.class.getDeclaredField("longs").getGenericType());
        assertNotEquals(new Variable("values", strings), new Variable("values", longs));
        assertNotEquals(new Variable("values", GenericTypeFactory.build(long[].class)), new Variable("values", GenericTypeFactory.build(Long[].class)));
    }

    @Test
    void nonBoxingTypesRetainTheirOriginalEnvironmentEquality() {
        GenericType first = scopedType(String.class, true);
        GenericType second = scopedType(String.class, true);
        assertEquals(new Variable("value", first), new Variable("value", first));
        assertNotEquals(new Variable("value", first), new Variable("value", second));
    }

    @Test
    void unresolvedTypesAreNotNormalizedByName() {
        assertNotEquals(new Variable("value", scopedType(long.class, false)), new Variable("value", GenericTypeFactory.build(Long.class)));
        assertNotEquals(new Variable("value", scopedType(Long.class, false)), new Variable("value", scopedType(Long.class, false)));
    }

    private GenericType scopedType(Class<?> type, boolean resolved) {
        return new GenericType(type, Collections.emptyMap()) {

            @Override
            public boolean isResolved() {
                return resolved;
            }

            @Override
            public boolean equals(Object other) {
                return this == other;
            }

            @Override
            public int hashCode() {
                return System.identityHashCode(this);
            }
        };
    }

    static class Fields {

        List<String> strings;

        List<Long> longs;
    }
}
