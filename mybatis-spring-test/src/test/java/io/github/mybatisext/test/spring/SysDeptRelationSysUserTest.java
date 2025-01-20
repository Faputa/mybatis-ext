package io.github.mybatisext.test.spring;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SysDeptRelationSysUserTest {

    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Test
    public void test() {
        List<SysDept> sysDepts = sysDeptMapper.selectSysDept();
        List<SysDept> sysDepts2 = sysDeptMapper.selectSysDeptLazy();
        List<SysDept> sysDepts3 = sysDeptMapper.selectSysDeptJoin();
        System.out.println(sysDepts.size());
        System.out.println(sysDepts3.size());
        System.out.println(sysDepts2.size());
    }
}
