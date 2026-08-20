package io.github.mybatisext.test.spring;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDeptMapper {

    List<SysDept> selectDepartmentsWithNestedUsers();

    List<SysDept> selectDepartmentsWithDottedColumnKey();

    List<SysDept> selectDepartmentsWithLazyUsers();

    List<SysDept> selectDepartmentsWithJoinedUsers();
}
