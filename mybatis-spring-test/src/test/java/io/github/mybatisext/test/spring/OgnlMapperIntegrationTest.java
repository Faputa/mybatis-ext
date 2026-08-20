package io.github.mybatisext.test.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class OgnlMapperIntegrationTest {
    public static ThreadLocal<Configuration> configuration = new ThreadLocal<>();
    public static ThreadLocal<List<Object>> parameterObjects = ThreadLocal.withInitial(ArrayList::new);

    @Autowired
    private OgnlTestMapper ognlTestMapper;
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @Test
    void expandsObjectIntoForeachItems() {
        // 测试foreach结构中是否可以使用OGNL方法将对象转换成列表
        // 考虑一种特殊的情况，对象某个列表属性对应的列属于该对象对应的表，那么一个该对象可能对应着表中的多个行，此时需要考虑批量插入和更新，所以需要将该对象转换为列表
        List<Object> list = ognlTestMapper.expandObjectForForeach("abc");
        assertEquals(Arrays.asList("abc", "abc"), list);
    }

    @Test
    void passesVarargsToStaticOgnlMethod() {
        // 测试OGNL方法是否支持变长参数
        List<Object> list = ognlTestMapper.convertVarargsToList("abc", "def");
        assertEquals(Arrays.asList("abc", "def"), list);
    }

    @Test
    void invokesMappedStatementFromStaticOgnlMethod() {
        configuration.set(sqlSessionTemplate.getConfiguration());
        try {
            // 测试OGNL方法是否可以内部调用MapperStatement
            // 考虑级联更新或删除的情况，在更新或删除之前需要先查询
            List<Object> list = ognlTestMapper.invokeMappedStatementFromOgnl();
            assertEquals(Arrays.asList("abc", "def"), list);
        } finally {
            configuration.remove();
        }
    }

    @Test
    void evaluatesRepeatedBindAssignments() {
        assertEquals(4, ognlTestMapper.evaluateRepeatedBindings());
    }

    @Test
    void exposesExpectedMybatisParameterObjectShapes() {
        SysDept department = new SysDept();
        Map<String, Object> map = new HashMap<>();
        List<Object> list = new ArrayList<>();
        Set<Object> set = new HashSet<>();
        Object[] array = new Object[0];
        parameterObjects.get().clear();
        try {
            assertEquals(1, ognlTestMapper.captureRawScalarParameter(1));
            assertEquals(1, ognlTestMapper.captureNamedScalarParameter(1));
            assertEquals(1, ognlTestMapper.captureMultipleNamedParameters(1, 2));
            assertEquals(1, ognlTestMapper.captureBeanParameter(department));
            assertEquals(1, ognlTestMapper.captureMapParameter(map));
            assertEquals(1, ognlTestMapper.captureListParameter(list));
            assertEquals(1, ognlTestMapper.captureSetParameter(set));
            assertEquals(1, ognlTestMapper.captureArrayParameter(array));

            List<Object> parameters = parameterObjects.get();
            assertEquals(8, parameters.size());
            assertEquals(1, parameters.get(0));
            assertMapEntry(parameters.get(1), "a", 1);
            assertMapEntry(parameters.get(2), "a", 1);
            assertMapEntry(parameters.get(2), "b", 2);
            assertSame(department, parameters.get(3));
            assertSame(map, parameters.get(4));
            assertMapEntry(parameters.get(5), "list", list);
            assertMapEntry(parameters.get(6), "collection", set);
            assertMapEntry(parameters.get(7), "array", array);
        } finally {
            parameterObjects.remove();
        }
    }

    private void assertMapEntry(Object parameter, String key, Object value) {
        assertTrue(parameter instanceof Map);
        assertEquals(value, ((Map<?, ?>) parameter).get(key));
    }
}
