package io.github.mybatisext.test.flex;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import com.mybatisflex.core.BaseMapper;

import io.github.mybatisext.annotation.MapTable;

@Mapper
@MapTable(Camera.class)
public interface CameraMapper extends BaseMapper<Camera> {

    long countCamera();

    Optional<Long> countByOrgId(int orgId);

    List<Camera> listByOrgId(int orgId);

    Camera listOneByOrgIdOrderByCameraId(int orgId);
}
