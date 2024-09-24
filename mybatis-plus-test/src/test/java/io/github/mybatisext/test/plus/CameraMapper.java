package io.github.mybatisext.test.plus;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.dynamic.datasource.annotation.DS;

@Mapper
public interface CameraMapper extends CameraMapper0 {

    long countCamera();

    @DS("ds2")
    Optional<Long> countByOrgId(int orgId);

    List<Camera> listByOrgId(int orgId);

    Camera listOneByOrgIdOrderByCameraId(int orgId);
}
