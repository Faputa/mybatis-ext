package com.mybatisext.test.plus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootApplication
@SpringBootTest
public class MybatisPlusTest {

	@Autowired
	private DemoMapper demoMapper;

	@Test
	public void test() {
		System.out.println("########" + demoMapper.countDidaTask());
		// 预期10000
		System.out.println("########" + demoMapper.selectCount(null));
		System.out.println("########" + demoMapper.countDidaTask2());
	}

}
