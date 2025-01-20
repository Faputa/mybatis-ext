package io.github.mybatisext.test.spring;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.ibatis.binding.BindingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MybatisSpringBootTest {

	@Autowired
	private SysUserMapper sysUserMapper;

	@Test
	public void test() {
		System.out.println("########" + sysUserMapper.countSysUser());
		assertThrows(BindingException.class, () -> sysUserMapper.countSysUser2());
	}
}
