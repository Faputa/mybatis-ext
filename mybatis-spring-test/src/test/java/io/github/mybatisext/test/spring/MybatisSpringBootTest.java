package io.github.mybatisext.test.spring;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.ibatis.binding.BindingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MybatisSpringBootTest {

	@Autowired
	private CameraMapper cameraMapper;

	@Test
	public void test() {
		System.out.println("########" + cameraMapper.countCamera());
		assertThrows(BindingException.class, () -> cameraMapper.countCamera2());
	}
}
