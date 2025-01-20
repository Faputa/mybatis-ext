package io.github.mybatisext.test.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    // 不注释会报错
    // long countSysUser();

    long countByDeptId(long deptId);
}
