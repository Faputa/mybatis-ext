package io.github.mybatisext.test.spring;

import java.util.List;

import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestOgnlTest {
    public static ThreadLocal<Configuration> configuration = new ThreadLocal<>();

    @Autowired
    private TestOgnlMapper testOgnlMapper;
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @Test
    void testOgnlInForeach() {
        // 测试foreach结构中是否可以使用OGNL方法将对象转换成列表
        // 考虑一种特殊的情况，对象某个列表属性对应的列属于该对象对应的表，那么一个该对象可能对应着表中的多个行，此时需要考虑批量插入和更新，所以需要将该对象转换为列表
        List<Object> list = testOgnlMapper.testOgnlInForeach("abc");
        System.out.println(list);
    }

    @Test
    void testOgnlVarags() {
        // 测试OGNL方法是否支持变长参数
        List<Object> list = testOgnlMapper.testVaragsToList("abc", "def");
        System.out.println(list);
    }

    @Test
    void testOgnlCallMapperStatement() {
        configuration.set(sqlSessionTemplate.getConfiguration());
        // 测试OGNL方法是否可以内部调用MapperStatement
        // 考虑级联更新或删除的情况，在更新或删除之前需要先查询
        List<Object> list = testOgnlMapper.testOgnlCallMapperStatement();
        System.out.println(list);
    }
}
