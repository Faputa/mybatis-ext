package io.github.mybatisext.test.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        List<SysDept> sysDepts1 = sysDeptMapper.selectSysDeptDot();
        List<SysDept> sysDepts2 = sysDeptMapper.selectSysDeptLazy();
        List<SysDept> sysDepts3 = sysDeptMapper.selectSysDeptJoin();
        System.out.println(sysDepts.size());
        System.out.println(sysDepts1.size());
        System.out.println(sysDepts2.size());
        System.out.println(sysDepts3.size());
        assertEquals(2, sysDepts.stream().mapToLong(v -> v.getUsers().size()).sum());
        assertEquals(2, sysDepts1.stream().mapToLong(v -> v.getUsers().size()).sum());
        assertEquals(2, sysDepts2.stream().mapToLong(v -> v.getUsers().size()).sum());
        assertEquals(2, sysDepts3.stream().mapToLong(v -> v.getUsers().size()).sum());
    }
}
