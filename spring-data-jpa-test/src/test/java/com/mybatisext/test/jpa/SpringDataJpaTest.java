package com.mybatisext.test.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootApplication
@SpringBootTest
public class SpringDataJpaTest {

	@Autowired
	private DemoRepository demoRepository;

	@Test
	public void test() {
		System.out.println("########" + demoRepository.countByOrgId(13L));
	}

}
