package io.github.mybatisext.test.flex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.mybatisflex.core.query.QueryWrapper;

@SpringBootTest
public class MybatisFlexTest {

	@Autowired
	private SysUserMapper sysUserMapper;

	@Test
	public void test() {
		System.out.println("########" + sysUserMapper.countSysUser());
		// 预期10000
		System.out.println("########" + sysUserMapper.selectCountByQuery(new QueryWrapper()));
		System.out.println("########" + sysUserMapper.countByDeptId(103));
		System.out.println("########" + sysUserMapper.listByDeptId(103));
		System.out.println("########" + sysUserMapper.listTop10ByDeptIdOrderByUserId(103));
	}
}
