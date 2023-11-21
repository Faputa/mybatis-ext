package com.example;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoRepository extends JpaRepository<Demo, Long> {

    // 不注释会报错
    // long countDidaTask();

    long countByOrgId(Long orgId);
}
