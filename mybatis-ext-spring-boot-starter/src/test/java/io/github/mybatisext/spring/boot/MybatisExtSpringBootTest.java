package io.github.mybatisext.spring.boot;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.mybatisext.test.SysDept;
import io.github.mybatisext.test.SysDeptMapper;
import io.github.mybatisext.test.SysMenu;
import io.github.mybatisext.test.SysMenuMapper;
import io.github.mybatisext.test.SysPost;
import io.github.mybatisext.test.SysPostMapper;
import io.github.mybatisext.test.SysRole;
import io.github.mybatisext.test.SysRoleMapper;
import io.github.mybatisext.test.SysUser;
import io.github.mybatisext.test.SysUserMapper;

@SpringBootTest
public class MybatisExtSpringBootTest {

	@Autowired
	private SysDeptMapper sysDeptMapper;
	@Autowired
	private SysMenuMapper sysMenuMapper;
	@Autowired
	private SysPostMapper sysPostMapper;
	@Autowired
	private SysRoleMapper sysRoleMapper;
	@Autowired
	private SysUserMapper sysUserMapper;

	@Test
	public void test() {
		List<SysDept> depts = sysDeptMapper.list(new SysDept());
		List<SysMenu> menus = sysMenuMapper.list(new SysMenu());
		List<SysPost> posts = sysPostMapper.list(new SysPost());
		List<SysRole> roles = sysRoleMapper.list(new SysRole());
		List<SysUser> users = sysUserMapper.list(new SysUser());
		System.out.println("########" + depts);
		System.out.println("########" + menus);
		System.out.println("########" + posts);
		System.out.println("########" + roles);
		System.out.println("########" + users);
	}
}
