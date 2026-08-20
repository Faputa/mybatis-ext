package io.github.mybatisext.test.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.ibatis.binding.BindingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MapperRegistrationSpringBootIntegrationTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    public void loadsMappedStatementsAndRejectsUnmappedMethods() {
        assertEquals(2L, sysUserMapper.countAllUsers());
        assertThrows(BindingException.class, () -> sysUserMapper.countUnmappedUsers());
    }
}
