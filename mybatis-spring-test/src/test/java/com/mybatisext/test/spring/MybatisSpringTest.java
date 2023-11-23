package com.mybatisext.test.spring;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.ibatis.binding.BindingException;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MybatisSpringTest {

	@Test
	public void test() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		DemoMapper demoMapper = context.getBean(DemoMapper.class);
		System.out.println("########" + demoMapper.countDidaTask());
		assertThrows(BindingException.class, () -> demoMapper.countDidaTask2());
		context.close();
	}

}
