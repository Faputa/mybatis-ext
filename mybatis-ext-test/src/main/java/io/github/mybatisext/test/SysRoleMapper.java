package io.github.mybatisext.test;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import io.github.mybatisext.mapper.BaseMapper;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    List<SysRole> list(SysRoleVO sysRoleVO);
}
