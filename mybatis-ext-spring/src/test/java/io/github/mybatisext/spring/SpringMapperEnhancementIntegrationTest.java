package io.github.mybatisext.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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

class SpringMapperEnhancementIntegrationTest {

    @Test
    void enhancesMappersThroughExtSqlSessionFactoryBean() {
        assertMapperQueries("classpath:applicationContext.xml");
    }

    @Test
    void enhancesMappersByReplacingStandardMybatisBean() {
        assertMapperQueries("classpath:applicationContextUsage2.xml");
    }

    private void assertMapperQueries(String contextResource) {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(contextResource)) {
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

            assertFalse(depts.isEmpty());
            assertFalse(menus.isEmpty());
            assertFalse(posts.isEmpty());
            assertFalse(roles.isEmpty());
            assertEquals(2, users.size());
            SysUser admin = users.stream().filter(user -> "admin".equals(user.getLoginName())).findFirst().orElse(null);
            assertNotNull(admin);
            assertEquals("研发部门", admin.getDept().getDeptName());
        }
    }
}
