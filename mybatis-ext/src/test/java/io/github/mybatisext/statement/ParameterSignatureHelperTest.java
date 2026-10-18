package io.github.mybatisext.statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericTypeFactory;

class ParameterSignatureHelperTest {

    @Test
    void ignoresSpecialParametersAndKeepsAnnotatedAndGenericNames() {
        ParameterSignature signature = ParameterSignatureHelper.buildParameterSignature(new Configuration(), method("query"));

        assertEquals(MapperMethod.ParamMap.class, signature.getType());
        assertEquals(List.class, signature.getNameToType().get("ids"));
        assertEquals(List.class, signature.getNameToType().get("param1"));
        assertEquals(2, signature.getNameToType().size());
    }

    @Test
    void distinguishesCollectionAndArrayParameterShapes() {
        ParameterSignature collection = ParameterSignatureHelper.wrapToMapIfCollection(List.class, "ids");
        ParameterSignature array = ParameterSignatureHelper.wrapToMapIfCollection(String[].class, "ids");

        assertTrue(collection.getNameToType().keySet().containsAll(Arrays.asList("collection", "list", "ids")));
        assertTrue(array.getNameToType().keySet().containsAll(Arrays.asList("array", "ids")));
        assertFalse(array.getNameToType().containsKey("collection"));
    }

    @Test
    void matchesParamMapOnlyWhenNamesAndRuntimeTypesMatchExactly() {
        ParameterSignature signature = ParameterSignatureHelper.buildParameterSignature(new Configuration(), method("query"));
        MapperMethod.ParamMap<Object> params = new MapperMethod.ParamMap<>();
        List<Long> ids = Arrays.asList(1L, 2L);
        params.put("ids", ids);
        params.put("param1", ids);

        assertTrue(ParameterSignatureHelper.isParameterSignatureMatch(params, signature));

        params.put("ids", null);
        assertTrue(ParameterSignatureHelper.isParameterSignatureMatch(params, signature));

        params.put("ids", "wrong-type");
        assertFalse(ParameterSignatureHelper.isParameterSignatureMatch(params, signature));

        params.put("ids", ids);
        params.put("unexpected", ids);
        assertFalse(ParameterSignatureHelper.isParameterSignatureMatch(params, signature));
    }

    @Test
    void serializesSignaturesAndRejectsMalformedInput() {
        ParameterSignature signature = ParameterSignatureHelper.buildParameterSignature(new Configuration(), method("query"));
        String serialized = ParameterSignatureHelper.toString(signature);
        ParameterSignature restored = ParameterSignatureHelper.fromString(serialized);

        assertEquals(signature.getType(), restored.getType());
        assertEquals(signature.getNameToType(), restored.getNameToType());
        assertThrows(MybatisExtException.class, () -> ParameterSignatureHelper.fromString("missing-separator"));
        assertThrows(MybatisExtException.class, () -> ParameterSignatureHelper.fromString("java.lang.Object|invalid-pair"));
        assertThrows(MybatisExtException.class, () -> ParameterSignatureHelper.fromString("missing.Type|"));
    }

    private GenericMethod method(String name) {
        return Arrays.stream(GenericTypeFactory.build(SignatureMapper.class).getMethods()).filter(method -> method.getName().equals(name)).findFirst().orElseThrow(AssertionError::new);
    }

    @Test
    void matchesEmptySignatureOnlyForAbsentParameters() {
        ParameterSignature signature = ParameterSignatureHelper.fromString(ParameterSignatureHelper.toString(new ParameterSignature()));
        assertTrue(ParameterSignatureHelper.isParameterSignatureMatch(null, signature));
        assertFalse(ParameterSignatureHelper.isParameterSignatureMatch("value", signature));
        assertFalse(ParameterSignatureHelper.isParameterSignatureMatch(new MapperMethod.ParamMap<>(), signature));
    }

    @Test
    void roundTripsPrimitiveSignaturesAndMatchesBoxedValues() {
        Class<?>[] types = {boolean.class, byte.class, short.class, int.class, long.class, float.class, double.class, char.class};
        Object[] values = {true, (byte) 1, (short) 1, 1, 1L, 1F, 1D, 'a'};
        for (int i = 0; i < types.length; i++) {
            ParameterSignature scalar = ParameterSignatureHelper.wrapToMapIfCollection(types[i], null);
            ParameterSignature restored = ParameterSignatureHelper.fromString(ParameterSignatureHelper.toString(scalar));
            assertEquals(types[i], restored.getType());
            assertTrue(ParameterSignatureHelper.isParameterSignatureMatch(values[i], restored));
            assertFalse(ParameterSignatureHelper.isParameterSignatureMatch("wrong", restored));

            ParameterSignature named = new ParameterSignature();
            named.setType(MapperMethod.ParamMap.class);
            named.getNameToType().put("value", types[i]);
            MapperMethod.ParamMap<Object> params = new MapperMethod.ParamMap<>();
            params.put("value", values[i]);
            assertTrue(ParameterSignatureHelper.isParameterSignatureMatch(params, ParameterSignatureHelper.toString(named)));
            params.put("value", "wrong");
            assertFalse(ParameterSignatureHelper.isParameterSignatureMatch(params, named));
        }
    }

    @Test
    void preservesDeclaredPrimitiveAndWrapperNamesWhenBuildingSignatures() {
        ParameterSignature primitive = ParameterSignatureHelper.buildParameterSignature(new Configuration(), method("primitive"));
        ParameterSignature boxed = ParameterSignatureHelper.buildParameterSignature(new Configuration(), method("boxed"));
        ParameterSignature named = ParameterSignatureHelper.buildParameterSignature(new Configuration(), method("namedPrimitive"));
        assertEquals("long|", ParameterSignatureHelper.toString(primitive));
        assertEquals("java.lang.Long|", ParameterSignatureHelper.toString(boxed));
        assertEquals(long.class, named.getNameToType().get("id"));
        assertEquals(long.class, named.getNameToType().get("param1"));
    }

    @Test
    void matchesReferenceSupertypesAndArrays() {
        assertTrue(ParameterSignatureHelper.isParameterSignatureMatch(1, "java.lang.Number|"));
        assertFalse(ParameterSignatureHelper.isParameterSignatureMatch("one", "java.lang.Number|"));
        assertTrue(ParameterSignatureHelper.isParameterSignatureMatch(Arrays.asList(1), "java.util.List|"));
        assertTrue(ParameterSignatureHelper.isParameterSignatureMatch(new Integer[]{1}, "[Ljava.lang.Number;|"));
        assertFalse(ParameterSignatureHelper.isParameterSignatureMatch(new String[]{"one"}, "[Ljava.lang.Number;|"));
        assertTrue(ParameterSignatureHelper.isParameterSignatureMatch(new int[]{1}, "[I|"));
        assertFalse(ParameterSignatureHelper.isParameterSignatureMatch(new long[]{1}, "[I|"));
    }

    interface SignatureMapper {

        void query(@Param("ids") List<Long> ids, RowBounds rowBounds);

        void primitive(long id);

        void boxed(Long id);

        void namedPrimitive(@Param("id") long id);
    }
}
