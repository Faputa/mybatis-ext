package io.github.mybatisext.spring.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.mybatisext.test.SysDept;
import io.github.mybatisext.test.SysDeptMapper;
import io.github.mybatisext.test.SysDeptVO;
import io.github.mybatisext.test.SysMenu;
import io.github.mybatisext.test.SysMenuMapper;
import io.github.mybatisext.test.SysMenuVO;
import io.github.mybatisext.test.SysPost;
import io.github.mybatisext.test.SysPostMapper;
import io.github.mybatisext.test.SysPostVO;
import io.github.mybatisext.test.SysRole;
import io.github.mybatisext.test.SysRoleMapper;
import io.github.mybatisext.test.SysRoleVO;
import io.github.mybatisext.test.SysUser;
import io.github.mybatisext.test.SysUserMapper;
import io.github.mybatisext.test.SysUserVO;

@SpringBootTest
public class DtoProjectionAutoConfigurationIntegrationTest {

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
    public void supportsDtoConditionsAndDtoProjection() {
        List<SysDept> depts = sysDeptMapper.list(new SysDeptVO());
        List<SysMenu> menus = sysMenuMapper.list(new SysMenuVO());
        List<SysPost> posts = sysPostMapper.list(new SysPostVO());
        List<SysRole> roles = sysRoleMapper.list(new SysRoleVO());
        List<SysUser> users = sysUserMapper.list(new SysUserVO());
        List<SysUserVO> sysUserVOS = sysUserMapper.listSysUserVO(new SysUser());

        assertFalse(depts.isEmpty());
        assertFalse(menus.isEmpty());
        assertFalse(posts.isEmpty());
        assertFalse(roles.isEmpty());
        assertEquals(2, users.size());
        assertEquals(2, sysUserVOS.size());
        SysUserVO user = sysUserVOS.stream().filter(item -> "ry".equals(item.getLoginName())).findFirst().orElse(null);
        assertNotNull(user);
        assertEquals(2L, user.getRoles2().get(0).getRoleId());
    }
}
