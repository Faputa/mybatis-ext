package io.github.mybatisext.test;

import java.util.Optional;

import io.github.mybatisext.mapper.BaseMapper;

public interface BaseSysUserMapper extends BaseMapper<SysUser> {

    long countAllUsers();

    Optional<Long> countByCreateBy(String createBy);

    int executeTwoDeleteStatements();
}
