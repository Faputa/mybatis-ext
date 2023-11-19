package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.mybatisflex.core.query.QueryWrapper;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(DemoApplication.class, args);
		DemoMapper demoMapper = context.getBean(DemoMapper.class);
		System.out.println("########" + demoMapper.countDidaTask());
		// 预期10000
		System.out.println("########" + demoMapper.selectCountByQuery(new QueryWrapper()));
		System.out.println("########" + demoMapper.countDidaTask2());
	}

}
