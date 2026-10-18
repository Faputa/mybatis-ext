package io.github.mybatisext.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class TypeUtilsTest {

    @Test
    void preservesPrimitiveWrappingContract() {
        Class<?>[][] pairs = {
                {boolean.class, Boolean.class},
                {byte.class, Byte.class},
                {short.class, Short.class},
                {int.class, Integer.class},
                {long.class, Long.class},
                {float.class, Float.class},
                {double.class, Double.class},
                {char.class, Character.class}
        };
        for (Class<?>[] pair : pairs) {
            assertSame(pair[1], TypeUtils.wrapPrimitive(pair[0]));
            assertSame(pair[1], TypeUtils.wrapPrimitive(pair[1]));
        }
        for (Class<?> type : new Class<?>[]{void.class, Void.class, String.class, List.class, long[].class, Long[].class}) {
            assertSame(type, TypeUtils.wrapPrimitive(type));
        }
    }

    @Test
    void boxingComparisonAndHashHandleNullTypes() {
        JavaType type = GenericTypeFactory.build(Long.class);
        assertTrue(TypeUtils.equalsIgnoreBoxing(null, null));
        assertFalse(TypeUtils.equalsIgnoreBoxing(null, type));
        assertFalse(TypeUtils.equalsIgnoreBoxing(type, null));
        assertEquals(0, TypeUtils.hashCodeIgnoreBoxing(null));
    }

    @Test
    void resolvesPrimitiveAndReferenceNamesWithoutChangingFailureBehavior() throws Exception {
        for (Class<?> type : new Class<?>[]{boolean.class, byte.class, short.class, int.class, long.class, float.class, double.class, char.class, Long.class, String.class, Map.class, long[].class, Long[].class}) {
            assertSame(type, TypeUtils.resolveClass(type.getName()));
        }
        assertThrows(ClassNotFoundException.class, () -> TypeUtils.resolveClass("void"));
        assertThrows(ClassNotFoundException.class, () -> TypeUtils.resolveClass("long[]"));
        assertThrows(ClassNotFoundException.class, () -> TypeUtils.resolveClass("missing.Type"));
    }

    class MapClass extends LinkedHashMap<String, Integer> {
    }

    interface MapInterface extends Map<String, Integer> {
    }

    @Test
    void resolvesTypeArgumentsFromClassesInterfacesAndTypes() {
        assertEquals(String.class, TypeUtils.resolveTypeArgument(MapClass.class, Map.class, 0));
        assertEquals(Integer.class, TypeUtils.resolveTypeArgument(MapClass.class, Map.class, 1));
        assertEquals(String.class, TypeUtils.resolveTypeArgument(MapInterface.class, Map.class, 0));
        assertEquals(Integer.class, TypeUtils.resolveTypeArgument(MapInterface.class, Map.class, 1));
        assertEquals(String.class, TypeUtils.resolveTypeArgument(MapClass.class.getGenericSuperclass(), Map.class, 0));
        assertEquals(Integer.class, TypeUtils.resolveTypeArgument(MapClass.class.getGenericSuperclass(), Map.class, 1));
    }
}
