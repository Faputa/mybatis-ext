package io.github.mybatisext.test.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserDepartmentAssociationMappingIntegrationTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    public void mapsDepartmentAssociationsWithNestedLazyAndJoinStrategies() {
        List<SysUser> sysUsers = sysUserMapper.selectUsersWithNestedDepartment();
        List<SysUser> sysUsers2 = sysUserMapper.selectUsersWithLazyDepartment();
        List<SysUser> sysUsers3 = sysUserMapper.selectUsersWithJoinedDepartment();
        assertDepartmentRelations(sysUsers);
        assertDepartmentRelations(sysUsers2);
        assertDepartmentRelations(sysUsers3);
    }

    private void assertDepartmentRelations(List<SysUser> users) {
        assertEquals(2, users.size());
        assertUserDepartment(findUser(users, 1L), 1L, "admin", 103L, "研发部门");
        assertUserDepartment(findUser(users, 2L), 2L, "ry", 105L, "测试部门");
    }

    private SysUser findUser(List<SysUser> users, long userId) {
        return users.stream().filter(v -> v.getUserId() == userId).findFirst().orElseThrow(AssertionError::new);
    }

    private void assertUserDepartment(SysUser user, long userId, String loginName, long deptId, String deptName) {
        assertEquals(userId, user.getUserId());
        assertEquals(loginName, user.getLoginName());
        assertEquals(deptId, user.getDept().getDeptId());
        assertEquals(deptName, user.getDept().getDeptName());
        assertEquals(deptName, user.getDeptName());
    }
}
