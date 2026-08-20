package io.github.mybatisext.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MethodSignatureTest {

    @Test
    void matchesMethodNameAndExactParameterTypes() throws Exception {
        MethodSignature signature = new MethodSignature("find", new Class[]{String.class, int.class});

        assertTrue(signature.match(SampleMethods.class.getDeclaredMethod("find", String.class, int.class)));
        assertFalse(signature.match(SampleMethods.class.getDeclaredMethod("find", String.class)));
        assertFalse(signature.match(SampleMethods.class.getDeclaredMethod("other", String.class, int.class)));
    }

    @Test
    void distinguishesPrimitiveAndWrapperParameters() throws Exception {
        MethodSignature signature = new MethodSignature("number", new Class[]{Integer.class});

        assertTrue(signature.match(SampleMethods.class.getDeclaredMethod("number", Integer.class)));
        assertFalse(signature.match(SampleMethods.class.getDeclaredMethod("number", int.class)));
    }

    static class SampleMethods {
        void find(String value, int limit) {
        }

        void find(String value) {
        }

        void other(String value, int limit) {
        }

        void number(Integer value) {
        }

        void number(int value) {
        }
    }
}
