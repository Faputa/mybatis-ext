package io.github.mybatisext.test.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
		System.out.println("########" + sysDeptMapper.list(new SysDept()));
		System.out.println("########" + sysMenuMapper.list(new SysMenu()));
		System.out.println("########" + sysPostMapper.list(new SysPost()));
		System.out.println("########" + sysRoleMapper.list(new SysRole()));
		System.out.println("########" + sysUserMapper.list(new SysUser()));
	}
}
