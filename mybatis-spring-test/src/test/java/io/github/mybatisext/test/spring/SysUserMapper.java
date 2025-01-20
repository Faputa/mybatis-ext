package io.github.mybatisext.test.spring;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper {

    long countSysUser();

    long countSysUser2();

    List<SysUser> selectSysUser();

    List<SysUser> selectSysUserLazy();

    List<SysUser> selectSysUserJoin();
}
