package io.github.mybatisext.spring.boot;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
public class MybatisExtBeanTest {

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void test() {
        assertNotNull(applicationContext.getBean("mybatisExtProperties"));
        assertNotNull(applicationContext.getBean("mybatisExtDialectSelector"));
        assertNotNull(applicationContext.getBean("mybatisExtContext"));
        assertNotNull(applicationContext.getBean("mybatisExtBeanPostProcessor"));
        assertNotNull(applicationContext.getBean("mybatisExtMapperMethodValidator"));
        assertNotNull(applicationContext.getBean("mybatisExtMapperMethodValidatorSecond"));
    }
}
