package io.github.mybatisext.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mybatisext.metadata.ColumnPermission;
import io.github.mybatisext.metadata.RowPermission;

public class ParameterizedTypeTest {

    interface A extends List<RowPermission> {
    }

    interface B extends List<ColumnPermission> {
    }

    @Test
    void test() {
        assertNotEquals(A.class.getGenericInterfaces()[0], B.class.getGenericInterfaces()[0]);
        assertEquals(A.class.getInterfaces()[0].getTypeParameters()[0], B.class.getInterfaces()[0].getTypeParameters()[0]);
    }
}
