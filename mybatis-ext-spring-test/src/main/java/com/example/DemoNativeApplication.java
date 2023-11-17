package com.example;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DemoNativeApplication {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		DemoMapper demoMapper = context.getBean(DemoMapper.class);
		System.out.println("########" + demoMapper.countDidaTask());
		System.out.println("########" + demoMapper.countDidaTask2());
		context.close();
	}

}
