package io.github.mybatisext.test.spring;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.ibatis.binding.BindingException;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MybatisSpringTest {

	@Test
	public void test() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		CameraMapper cameraMapper = context.getBean(CameraMapper.class);
		System.out.println("########" + cameraMapper.countCamera());
		assertThrows(BindingException.class, () -> cameraMapper.countCamera2());
		context.close();
	}
}
