package io.github.mybatisext.test.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class MybatisExtSpringBootTest {

	// @Autowired
	// private CameraMapper cameraMapper;
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
		// System.out.println("########" + cameraMapper.countCamera());
		// System.out.println("########" + cameraMapper.countByOrgId(16));
		// System.out.println("########" + cameraMapper.listByOrgId(16));
		// System.out.println("########" + cameraMapper.listOneByOrgIdOrderByCameraId(16));
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
