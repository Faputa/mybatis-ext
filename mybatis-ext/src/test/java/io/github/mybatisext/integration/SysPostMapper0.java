package io.github.mybatisext.integration;

import java.util.Optional;

import io.github.mybatisext.mapper.BaseMapper;

public interface SysPostMapper0 extends BaseMapper<SysPost> {

    long countSysPost();

    Optional<Long> countByCreateBy(String createBy);

    int deleteSysPostTwice();
}
