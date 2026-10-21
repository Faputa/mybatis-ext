package io.github.mybatisext.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ConfigurationInterface;

class MybatisExtBeanPostProcessorTest {

    @Test
    void enhancesOnlyFactoriesAndPreservesOtherMapValues() {
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(new Configuration());
        Map<String, Object> beans = new LinkedHashMap<>();
        beans.put("factory", factory);
        beans.put("description", "main database");
        beans.put("optional", null);
        MybatisExtBeanPostProcessor processor = new MybatisExtBeanPostProcessor();

        Map<?, ?> enhanced = (Map<?, ?>) processor.postProcessAfterInitialization(beans, "factories");
        assertTrue(((SqlSessionFactory) enhanced.get("factory")).getConfiguration() instanceof ConfigurationInterface);
        assertEquals("main database", enhanced.get("description"));
        assertTrue(enhanced.containsKey("optional"));
        assertNull(enhanced.get("optional"));
        assertSame(factory, beans.get("factory"));
        assertEquals(beans.keySet(), enhanced.keySet());
    }

    @Test
    void leavesMapsWithoutFactoriesUnchanged() {
        Map<String, Object> beans = new LinkedHashMap<>();
        beans.put("optional", null);
        assertSame(beans, new MybatisExtBeanPostProcessor().postProcessAfterInitialization(beans, "values"));
    }
}
