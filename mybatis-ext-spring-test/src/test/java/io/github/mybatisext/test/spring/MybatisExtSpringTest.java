package io.github.mybatisext.test.spring;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

public class MybatisExtSpringTest {

	@Test
	public void test() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		// CameraMapper cameraMapper = context.getBean(CameraMapper.class);
		// System.out.println("########" + cameraMapper.countCamera());
		// System.out.println("########" + cameraMapper.countByOrgId(16));
		// System.out.println("########" + cameraMapper.listByOrgId(16));
		// System.out.println("########" + cameraMapper.listOneByOrgIdOrderByCameraId(16));
		SysDeptMapper sysDeptMapper = context.getBean(SysDeptMapper.class);
		SysMenuMapper sysMenuMapper = context.getBean(SysMenuMapper.class);
		SysPostMapper sysPostMapper = context.getBean(SysPostMapper.class);
		SysRoleMapper sysRoleMapper = context.getBean(SysRoleMapper.class);
		SysUserMapper sysUserMapper = context.getBean(SysUserMapper.class);
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
		context.close();
	}
}
