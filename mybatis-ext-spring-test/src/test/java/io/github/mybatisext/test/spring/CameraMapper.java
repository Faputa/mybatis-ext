package io.github.mybatisext.test.spring;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import io.github.mybatisext.mapper.BaseMapper;

@Mapper
public interface CameraMapper extends BaseMapper<Camera> {

    long countCamera();

    Optional<Long> countByOrgId(int orgId);

    List<Camera> listByOrgId(int orgId);

    Camera listOneByOrgIdOrderByCameraId(int orgId);
}
