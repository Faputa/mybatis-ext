package com.mybatisext.test.flex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import com.mybatisflex.core.query.QueryWrapper;

@SpringBootApplication
@SpringBootTest
public class MybatisFlexTest {

	@Autowired
	private DemoMapper demoMapper;

	@Test
	public void test() {
		System.out.println("########" + demoMapper.countDidaTask());
		// 预期10000
		System.out.println("########" + demoMapper.selectCountByQuery(new QueryWrapper()));
		System.out.println("########" + demoMapper.countDidaTask2());
	}

}
