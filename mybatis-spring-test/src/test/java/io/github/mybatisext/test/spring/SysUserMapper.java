package io.github.mybatisext.test.spring;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper {

    long countAllUsers();

    long countUnmappedUsers();

    List<SysUser> selectUsersWithNestedDepartment();

    List<SysUser> selectUsersWithLazyDepartment();

    List<SysUser> selectUsersWithJoinedDepartment();
}
