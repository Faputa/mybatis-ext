package io.github.mybatisext.test;

import java.util.Optional;

import io.github.mybatisext.mapper.BaseMapper;

public interface SysUserMapper0 extends BaseMapper<SysUser> {

    long countSysUser();

    Optional<Long> countByCreateBy(String createBy);

    int deleteSysUserTwice();
}
