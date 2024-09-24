package io.github.mybatisext.test.spring;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MybatisExtSpringTest {

	@Test
	public void test() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		CameraMapper cameraMapper = context.getBean(CameraMapper.class);
		System.out.println("########" + cameraMapper.countCamera());
		System.out.println("########" + cameraMapper.countByOrgId(16));
		context.close();
	}
}
