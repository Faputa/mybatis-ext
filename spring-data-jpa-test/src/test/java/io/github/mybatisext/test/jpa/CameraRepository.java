package io.github.mybatisext.test.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CameraRepository extends JpaRepository<Camera, Long> {

    // 不注释会报错
    // long countCamera();

    long countByOrgId(Long orgId);
}
