package com.mybatisext.test.flex;

import org.apache.ibatis.annotations.Mapper;

import com.mybatisext.annotation.MapTable;
import com.mybatisflex.core.BaseMapper;

@Mapper
@MapTable(Camera.class)
public interface CameraMapper extends BaseMapper<Camera> {

    long countCamera();

    long countCamera2();

    // long countCamera3();
}
