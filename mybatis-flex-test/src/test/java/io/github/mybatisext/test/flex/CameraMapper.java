package io.github.mybatisext.test.flex;

import org.apache.ibatis.annotations.Mapper;

import io.github.mybatisext.annotation.MapTable;
import com.mybatisflex.core.BaseMapper;

@Mapper
@MapTable(Camera.class)
public interface CameraMapper extends BaseMapper<Camera> {

    long countCamera();

    long countCamera2();

    // long countCamera3();
}
