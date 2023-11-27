package io.github.mybatisext.test.spring;

import org.apache.ibatis.annotations.Mapper;

import io.github.mybatisext.mapper.BaseMapper;

@Mapper
public interface CameraMapper extends BaseMapper<Camera> {

    long countCamera();

    long countCamera2();
}
