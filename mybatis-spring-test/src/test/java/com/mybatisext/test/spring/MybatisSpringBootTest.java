package com.mybatisext.test.spring;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.ibatis.binding.BindingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootApplication
@SpringBootTest
public class MybatisSpringBootTest {

	@Autowired
	private DemoMapper demoMapper;

	@Test
	public void test() {
		System.out.println("########" + demoMapper.countDidaTask());
		assertThrows(BindingException.class, () -> demoMapper.countDidaTask2());
	}

}
