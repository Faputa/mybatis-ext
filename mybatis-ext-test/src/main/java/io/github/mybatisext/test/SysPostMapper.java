package io.github.mybatisext.test;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import io.github.mybatisext.mapper.BaseMapper;

@Mapper
public interface SysPostMapper extends BaseMapper<SysPost> {

    List<SysPost> list(SysPostVO sysPostVO);
}
