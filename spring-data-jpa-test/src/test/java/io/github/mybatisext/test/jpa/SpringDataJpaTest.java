package io.github.mybatisext.test.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SpringDataJpaTest {

	@Autowired
	private SysUserRepository sysUserRepository;

	@Test
	public void test() {
		System.out.println("########" + sysUserRepository.countByDeptId(103L));
	}
}
