package io.github.mybatisext.test.plus;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.dynamic.datasource.annotation.DS;

@Mapper
public interface CameraMapper extends CameraMapper0 {

    long countCamera();

    @DS("ds2")
    long countCamera2();

    // long countCamera3();
}
