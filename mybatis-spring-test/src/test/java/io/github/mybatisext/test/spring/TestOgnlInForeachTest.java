package io.github.mybatisext.test.spring;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestOgnlInForeachTest {

    @Autowired
    private TestOgnlInForeachMapper testOgnlInForeachMapper;

    @Test
    void test() {
        // 考虑一种特殊的情况，对象某个列表属性对应的列属于该对象对应的表，那么一个该对象可能对应着表中的多个行，此时需要考虑批量插入和更新，所以需要将该对象转换为列表
        List<String> list = testOgnlInForeachMapper.testOgnlInForeach("abc");
        System.out.println(list);
    }
}
