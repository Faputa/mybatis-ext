package io.github.mybatisext.test.spring;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDeptMapper {

    List<SysDept> selectSysDept();

    List<SysDept> selectSysDeptLazy();

    List<SysDept> selectSysDeptJoin();
}
