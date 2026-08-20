package io.github.mybatisext.test.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.ibatis.binding.BindingException;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MapperRegistrationXmlContextIntegrationTest {

    @Test
    public void loadsMappedStatementsAndRejectsUnmappedMethods() {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml")) {
            SysUserMapper sysUserMapper = context.getBean(SysUserMapper.class);
            assertEquals(2L, sysUserMapper.countAllUsers());
            assertThrows(BindingException.class, () -> sysUserMapper.countUnmappedUsers());
        }
    }
}
