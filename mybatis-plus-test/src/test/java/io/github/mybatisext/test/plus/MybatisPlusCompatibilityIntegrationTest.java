package io.github.mybatisext.test.plus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MybatisPlusCompatibilityIntegrationTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    public void supportsDerivedQueriesAlongsideMybatisPlus() {
        assertEquals(2L, sysUserMapper.countAllUsers());
        assertEquals(2L, sysUserMapper.selectCount(null));
        assertEquals(Optional.of(1L), sysUserMapper.countByDeptId(103));

        assertDeptUsers(sysUserMapper.listByDeptId(103));
        assertDeptUsers(sysUserMapper.listTop10ByDeptIdOrderByUserId(103));
    }

    private void assertDeptUsers(List<SysUser> users) {
        assertEquals(1, users.size());
        assertEquals(1L, users.get(0).getUserId());
        assertEquals(103L, users.get(0).getDeptId());
        assertEquals("admin", users.get(0).getLoginName());
    }
}
