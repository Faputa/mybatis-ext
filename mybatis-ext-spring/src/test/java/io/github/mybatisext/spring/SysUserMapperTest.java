package io.github.mybatisext.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.mybatisext.test.SysDept;
import io.github.mybatisext.test.SysUser;
import io.github.mybatisext.test.SysUserMapper;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class SysUserMapperTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    private SysUser testUser;

    @BeforeEach
    public void setUp() {
        testUser = new SysUser();
        testUser.setUserId(3L);
        testUser.setLoginName("testUser");
        testUser.setUserName("Test User");
        testUser.setUserType("00");
        testUser.setEmail("testuser@example.com");
        testUser.setPhonenumber("1234567890");
        testUser.setSex("M");
        testUser.setPassword("hashedPassword");
        testUser.setSalt("saltValue");
        testUser.setStatus("0");
        testUser.setDelFlag("0");
        testUser.setDept(new SysDept());
        testUser.setPostIds(new ArrayList<>());
        testUser.setRoleIds(new ArrayList<>());
        testUser.setRoles(new ArrayList<>());
    }

    @AfterEach
    public void tearDown() {
        if (sysUserMapper.exists(testUser)) {
            sysUserMapper.delete(testUser);
        }
    }

    @Test
    public void testSaveAndGetSysUser() {
        int rowsAffected = sysUserMapper.save(testUser);
        assertEquals(1, rowsAffected);

        SysUser retrievedUser = sysUserMapper.get(testUser);
        assertNotNull(retrievedUser);
        assertEquals(testUser.getUserId(), retrievedUser.getUserId());
        assertEquals(testUser.getLoginName(), retrievedUser.getLoginName());
    }

    @Test
    public void testSaveIgnoreNullAndGetSysUser() {
        int rowsAffected = sysUserMapper.saveIgnoreNull(testUser);
        assertEquals(1, rowsAffected);

        SysUser retrievedUser = sysUserMapper.get(testUser);
        assertNotNull(retrievedUser);
        assertEquals(testUser.getUserId(), retrievedUser.getUserId());
        assertEquals(testUser.getLoginName(), retrievedUser.getLoginName());
    }

    @Test
    public void testSaveBatchAndGetSysUser() {
        int rowsAffected = sysUserMapper.saveBatch(Collections.singletonList(testUser));
        assertEquals(1, rowsAffected);

        SysUser retrievedUser = sysUserMapper.get(testUser);
        assertNotNull(retrievedUser);
        assertEquals(testUser.getUserId(), retrievedUser.getUserId());
        assertEquals(testUser.getLoginName(), retrievedUser.getLoginName());
    }

    @Test
    public void testSaveBatchIgnoreNullAndGetSysUser() {
        int rowsAffected = sysUserMapper.saveBatchIgnoreNull(Collections.singletonList(testUser));
        assertEquals(1, rowsAffected);

        SysUser retrievedUser = sysUserMapper.get(testUser);
        assertNotNull(retrievedUser);
        assertEquals(testUser.getUserId(), retrievedUser.getUserId());
        assertEquals(testUser.getLoginName(), retrievedUser.getLoginName());
    }

    @Test
    public void testUpdateSysUser() {
        sysUserMapper.save(testUser);
        testUser.setUserName("Updated User Name");
        int rowsAffected = sysUserMapper.updateIgnoreNull(testUser);
        assertEquals(1, rowsAffected);

        SysUser updatedUser = sysUserMapper.get(testUser);
        assertEquals("Updated User Name", updatedUser.getUserName());
    }

    @Test
    public void testUpdateBatchSysUser() {
        sysUserMapper.save(testUser);
        testUser.setUserName("Updated User Name");
        int rowsAffected = sysUserMapper.updateBatch(Collections.singletonList(testUser));
        assertEquals(1, rowsAffected);

        SysUser updatedUser = sysUserMapper.get(testUser);
        assertEquals("Updated User Name", updatedUser.getUserName());
    }

    @Test
    public void testUpdateBatchIgnoreNullSysUser() {
        sysUserMapper.save(testUser);
        testUser.setUserName("Updated User Name");
        int rowsAffected = sysUserMapper.updateBatchIgnoreNull(Collections.singletonList(testUser));
        assertEquals(1, rowsAffected);

        SysUser updatedUser = sysUserMapper.get(testUser);
        assertEquals("Updated User Name", updatedUser.getUserName());
    }

    @Test
    public void testDeleteSysUser() {
        sysUserMapper.save(testUser);
        int rowsAffected = sysUserMapper.delete(testUser);
        assertEquals(1, rowsAffected);
        assertFalse(sysUserMapper.exists(testUser));
    }

    @Test
    public void testDeleteBatchSysUser() {
        sysUserMapper.save(testUser);
        int rowsAffected = sysUserMapper.deleteBatch(Collections.singletonList(testUser));
        assertEquals(1, rowsAffected);
        assertFalse(sysUserMapper.exists(testUser));
    }

    @Test
    public void testListSysUsers() {
        sysUserMapper.save(testUser);
        List<SysUser> users = sysUserMapper.list(new SysUser());
        assertTrue(users.contains(testUser));
    }

    @Test
    public void testCountSysUsers() {
        sysUserMapper.save(testUser);
        long count = sysUserMapper.count(new SysUser());
        assertTrue(count > 0);
    }
}
