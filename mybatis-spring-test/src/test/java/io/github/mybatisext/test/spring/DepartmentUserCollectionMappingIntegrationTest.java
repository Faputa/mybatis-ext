package io.github.mybatisext.test.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DepartmentUserCollectionMappingIntegrationTest {

    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Test
    public void mapsUserCollectionsWithNestedDottedLazyAndJoinStrategies() {
        List<SysDept> sysDepts = sysDeptMapper.selectDepartmentsWithNestedUsers();
        List<SysDept> sysDepts1 = sysDeptMapper.selectDepartmentsWithDottedColumnKey();
        List<SysDept> sysDepts2 = sysDeptMapper.selectDepartmentsWithLazyUsers();
        List<SysDept> sysDepts3 = sysDeptMapper.selectDepartmentsWithJoinedUsers();
        assertUserRelations(sysDepts);
        assertUserRelations(sysDepts1);
        assertUserRelations(sysDepts2);
        assertUserRelations(sysDepts3);
    }

    private void assertUserRelations(List<SysDept> departments) {
        assertEquals(10, departments.size());
        assertEquals(2, departments.stream().mapToLong(v -> v.getUsers().size()).sum());
        assertEquals(2, departments.stream().mapToLong(v -> v.getUserNames().size()).sum());
        assertDepartmentUser(departments, 103L, 1L, "admin", "若依");
        assertDepartmentUser(departments, 105L, 2L, "ry", "若依");
    }

    private void assertDepartmentUser(List<SysDept> departments, long deptId, long userId, String loginName, String userName) {
        SysDept department = departments.stream().filter(v -> v.getDeptId() == deptId).findFirst().orElseThrow(AssertionError::new);
        assertEquals(1, department.getUsers().size());
        assertEquals(userId, department.getUsers().get(0).getUserId());
        assertEquals(loginName, department.getUsers().get(0).getLoginName());
        assertEquals(java.util.Collections.singletonList(userName), department.getUserNames());
    }
}
