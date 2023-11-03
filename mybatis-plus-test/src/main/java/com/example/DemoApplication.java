package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(DemoApplication.class, args);
		DemoMapper demoMapper = context.getBean(DemoMapper.class);
		System.out.println("########" + demoMapper.countDidaTask());
		System.out.println("########" + demoMapper.selectCount(null));
	}

}
