package io.github.mybatisext.test;

import java.util.Optional;

import io.github.mybatisext.mapper.BaseMapper;

public interface CameraMapper0 extends BaseMapper<Camera> {

    long countCamera();

    Optional<Long> countByOrgId(int orgId);

    int deleteCameraTwice();
}
