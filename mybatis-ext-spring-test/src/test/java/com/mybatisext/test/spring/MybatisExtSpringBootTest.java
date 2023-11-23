package com.mybatisext.test.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootApplication
@SpringBootTest
public class MybatisExtSpringBootTest {

	@Autowired
	private DemoMapper demoMapper;

	@Test
	public void test() {
		System.out.println("########" + demoMapper.countDidaTask());
		System.out.println("########" + demoMapper.countDidaTask2());
	}

}
