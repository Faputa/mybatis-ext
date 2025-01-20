package io.github.mybatisext.test.spring;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SysUserRelationSysDeptTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    public void test() {
        List<SysUser> sysUsers = sysUserMapper.selectSysUser();
        List<SysUser> sysUsers2 = sysUserMapper.selectSysUserLazy();
        List<SysUser> sysUsers3 = sysUserMapper.selectSysUserJoin();
        System.out.println(sysUsers.size());
        System.out.println(sysUsers2.size());
        System.out.println(sysUsers3.size());
    }
}
