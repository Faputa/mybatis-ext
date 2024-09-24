package io.github.mybatisext.test.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MybatisExtSpringBootTest {

	@Autowired
	private CameraMapper cameraMapper;

	@Test
	public void test() {
		System.out.println("########" + cameraMapper.countCamera());
		System.out.println("########" + cameraMapper.countByOrgId(16));
		System.out.println("########" + cameraMapper.listByOrgId(16));
		System.out.println("########" + cameraMapper.listOneByOrgIdOrderByCameraId(16));
	}
}
