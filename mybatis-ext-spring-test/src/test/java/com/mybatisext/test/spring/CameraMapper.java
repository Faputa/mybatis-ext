package com.mybatisext.test.spring;

import org.apache.ibatis.annotations.Mapper;

import com.mybatisext.mapper.BaseMapper;

@Mapper
public interface CameraMapper extends BaseMapper<Camera> {

    long countCamera();

    long countCamera2();
}
