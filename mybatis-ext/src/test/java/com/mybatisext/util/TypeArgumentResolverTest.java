package com.mybatisext.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class TypeArgumentResolverTest {

    class MapClass extends HashMap<String, Integer> {
    }

    interface MapInterface extends Map<String, Integer> {
    }

    @Test
    void test() {
        assertEquals(String.class, TypeArgumentResolver.resolveTypeArgument(MapClass.class, HashMap.class, 0));
        assertEquals(Integer.class, TypeArgumentResolver.resolveTypeArgument(MapClass.class, HashMap.class, 1));
        assertEquals(String.class, TypeArgumentResolver.resolveTypeArgument(MapClass.class, Map.class, 0));
        assertEquals(Integer.class, TypeArgumentResolver.resolveTypeArgument(MapClass.class, Map.class, 1));
        assertEquals(String.class, TypeArgumentResolver.resolveTypeArgument(MapInterface.class, Map.class, 0));
        assertEquals(Integer.class, TypeArgumentResolver.resolveTypeArgument(MapInterface.class, Map.class, 1));
    }
}
