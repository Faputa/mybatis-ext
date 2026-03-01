package io.github.mybatisext.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class TypeUtilsTest {

    class MapClass extends LinkedHashMap<String, Integer> {
    }

    interface MapInterface extends Map<String, Integer> {
    }

    @Test
    void test() {
        assertEquals(String.class, TypeUtils.resolveTypeArgument(MapClass.class, Map.class, 0));
        assertEquals(Integer.class, TypeUtils.resolveTypeArgument(MapClass.class, Map.class, 1));
        assertEquals(String.class, TypeUtils.resolveTypeArgument(MapClass.class, Map.class, 0));
        assertEquals(Integer.class, TypeUtils.resolveTypeArgument(MapClass.class, Map.class, 1));
        assertEquals(String.class, TypeUtils.resolveTypeArgument(MapInterface.class, Map.class, 0));
        assertEquals(Integer.class, TypeUtils.resolveTypeArgument(MapInterface.class, Map.class, 1));
        assertEquals(String.class, TypeUtils.resolveTypeArgument(MapClass.class.getGenericSuperclass(), Map.class, 0));
        assertEquals(Integer.class, TypeUtils.resolveTypeArgument(MapClass.class.getGenericSuperclass(), Map.class, 1));
    }
}
