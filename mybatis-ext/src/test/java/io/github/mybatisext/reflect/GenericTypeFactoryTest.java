package io.github.mybatisext.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class GenericTypeFactoryTest {

    interface TestMap extends Map<String, List<Integer>> {
    }

    @Test
    void test() {
        GenericType genericType = GenericTypeFactory.build(TestMap.class);
        assertEquals(List.class, genericType.getGenericInterfaces()[0].getTypeParameters()[1].getType());
        assertEquals(Integer.class, genericType.getGenericInterfaces()[0].getTypeParameters()[1].getTypeParameters()[0].getType());
    }
}
