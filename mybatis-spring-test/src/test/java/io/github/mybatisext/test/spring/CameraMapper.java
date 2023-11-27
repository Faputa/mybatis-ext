package io.github.mybatisext.test.spring;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CameraMapper {

    long countCamera();

    long countCamera2();

    List<Camera> selectCamera();

    List<Camera> selectCameraLazy();

    List<Camera> selectCameraJoin();
}
