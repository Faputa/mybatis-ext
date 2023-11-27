package io.github.mybatisext.test.flex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.mybatisflex.core.query.QueryWrapper;

@SpringBootTest
public class MybatisFlexTest {

	@Autowired
	private CameraMapper cameraMapper;

	@Test
	public void test() {
		System.out.println("########" + cameraMapper.countCamera());
		// 预期10000
		System.out.println("########" + cameraMapper.selectCountByQuery(new QueryWrapper()));
		System.out.println("########" + cameraMapper.countCamera2());
	}
}
