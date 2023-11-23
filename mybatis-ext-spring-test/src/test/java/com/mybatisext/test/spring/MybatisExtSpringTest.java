package com.mybatisext.test.spring;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MybatisExtSpringTest {

	@Test
	public void test() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		DemoMapper demoMapper = context.getBean(DemoMapper.class);
		System.out.println("########" + demoMapper.countDidaTask());
		System.out.println("########" + demoMapper.countDidaTask2());
		context.close();
	}

}
