package io.github.mybatisext.test.plus;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.dynamic.datasource.annotation.DS;

@Mapper
public interface SysUserMapper extends SysUserMapper0 {

    long countSysUser();

    @DS("ds2")
    Optional<Long> countByDeptId(int deptId);

    List<SysUser> listByDeptId(int deptId);

    List<SysUser> listTop10ByDeptIdOrderByUserId(int deptId);
}
