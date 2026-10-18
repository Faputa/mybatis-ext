package io.github.mybatisext.ognl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;

import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.scripting.xmltags.ExpressionEvaluator;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.idgenerator.IdGenerator;

class OgnlTest {

    @Test
    void matchesAnyParameterSignatureThroughMyBatisExpressions() {
        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        String single = Ognl.IsParameterSignatureMatch + "(_parameter, 'java.lang.String|')";
        String multiple = Ognl.IsParameterSignatureMatch + "(_parameter, 'java.lang.String|', 'java.lang.Integer|')";

        assertTrue(evaluator.evaluateBoolean(single, Collections.singletonMap("_parameter", "value")));
        assertFalse(evaluator.evaluateBoolean(single, Collections.singletonMap("_parameter", 1)));
        assertTrue(evaluator.evaluateBoolean(multiple, Collections.singletonMap("_parameter", "value")));
        assertTrue(evaluator.evaluateBoolean(multiple, Collections.singletonMap("_parameter", 1)));
        assertFalse(evaluator.evaluateBoolean(multiple, Collections.singletonMap("_parameter", 1L)));
        assertFalse(evaluator.evaluateBoolean(multiple, Collections.singletonMap("_parameter", null)));

        String noArguments = Ognl.IsParameterSignatureMatch + "(_parameter, \"|\")";
        assertTrue(evaluator.evaluateBoolean(noArguments, Collections.singletonMap("_parameter", null)));
        assertFalse(evaluator.evaluateBoolean(noArguments, Collections.singletonMap("_parameter", "value")));

        MapperMethod.ParamMap<Object> parameters = new MapperMethod.ParamMap<>();
        parameters.put("id", null);
        String nullable = Ognl.IsParameterSignatureMatch + "(_parameter, 'org.apache.ibatis.binding.MapperMethod$ParamMap|id:java.lang.Long', 'org.apache.ibatis.binding.MapperMethod$ParamMap|id:java.lang.String')";
        assertTrue(evaluator.evaluateBoolean(nullable, Collections.singletonMap("_parameter", parameters)));
    }

    @Test
    void detectsEmptyValuesIncludingPrimitiveArrays() {
        assertTrue(Ognl.isEmpty(null));
        assertTrue(Ognl.isEmpty("  "));
        assertTrue(Ognl.isEmpty(Collections.emptyList()));
        assertTrue(Ognl.isEmpty(Collections.emptyMap()));
        assertTrue(Ognl.isEmpty(new Object[0]));
        assertTrue(Ognl.isEmpty(new int[0]));
        assertTrue(Ognl.isEmpty(new byte[0]));

        assertFalse(Ognl.isEmpty("value"));
        assertFalse(Ognl.isEmpty(Collections.singletonList("value")));
        assertFalse(Ognl.isEmpty(new int[]{1}));
        assertFalse(Ognl.isEmpty(0));
    }

    @Test
    void detectsNumericValues() {
        assertTrue(Ognl.isNumber(1));
        assertTrue(Ognl.isNumber("-1.5"));
        assertFalse(Ognl.isNumber(null));
        assertFalse(Ognl.isNumber("1a"));
        assertEquals(Ognl.isNotNumber("1a"), !Ognl.isNumber("1a"));
    }

    @Test
    void detectsReadableBeanProperties() throws Exception {
        assertTrue(Ognl.hasProperty(new Bean(), "name"));
        assertFalse(Ognl.hasProperty(new Bean(), "missing"));
        assertFalse(Ognl.hasProperty(null, "name"));
        assertTrue(Ognl.hasProperty(new HashMap<>(), "dynamicProperty"));
    }

    @Test
    void generatesUuidOnlyWhenValueIsEmpty() {
        String generated = Ognl.getUuid(null);

        assertEquals(32, generated.length());
        assertNotEquals(generated, Ognl.getUuid(null));
        assertEquals("preset", Ognl.getUuid("preset"));
    }

    @Test
    void invokesCustomGeneratorOnlyWhenValueIsEmpty() throws Exception {
        assertEquals("generated", Ognl.getCustomId(TestIdGenerator.class, null));
        assertEquals("preset", Ognl.getCustomId(TestIdGenerator.class, "preset"));
    }

    public static class Bean {

        public String getName() {
            return "name";
        }
    }

    public static class TestIdGenerator implements IdGenerator<String> {

        @Override
        public String getId() {
            return "generated";
        }
    }
}
