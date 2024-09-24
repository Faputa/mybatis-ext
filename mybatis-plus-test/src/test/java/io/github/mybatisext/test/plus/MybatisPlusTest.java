package io.github.mybatisext.test.plus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MybatisPlusTest {

	@Autowired
	private CameraMapper cameraMapper;

	@Test
	public void test() {
		System.out.println("########" + cameraMapper.countCamera());
		// 预期10000
		System.out.println("########" + cameraMapper.selectCount(null));
		System.out.println("########" + cameraMapper.countByOrgId(16));
		System.out.println("########" + cameraMapper.listByOrgId(16));
		System.out.println("########" + cameraMapper.listOneByOrgIdOrderByCameraId(16));
	}
}
