package io.github.mybatisext.test.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    long countByDeptId(long deptId);
}
