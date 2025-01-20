package io.github.mybatisext.test.flex;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import com.mybatisflex.core.BaseMapper;

import io.github.mybatisext.annotation.MapTable;

@Mapper
@MapTable(SysUser.class)
public interface SysUserMapper extends BaseMapper<SysUser> {

    long countSysUser();

    Optional<Long> countByDeptId(int deptId);

    List<SysUser> listByDeptId(int deptId);

    List<SysUser> listTop10ByDeptIdOrderByUserId(int deptId);
}
