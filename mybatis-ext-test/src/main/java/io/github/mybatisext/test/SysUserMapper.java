package io.github.mybatisext.test;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import io.github.mybatisext.mapper.BaseMapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    List<SysUser> list(SysUserVO sysUserVO);

    List<SysUserVO> listSysUserVO(SysUser sysUser);

    SysUser getByLoginName(@Param("loginName") String loginName);

    long countByStatus(@Param("status") String status);

    boolean existsByEmail(@Param("email") String email);

    List<SysUser> listTop1ByStatusOrderByUserIdDesc(@Param("status") String status);

    List<SysUser> listByUserIdInOrderByUserIdDesc(@Param("userId") List<Long> userId);

    List<SysUser> listByUserIdBetweenStartToEndOrderByUserId(@Param("start") Long start, @Param("end") Long end);

    List<SysUser> listByLoginNameOrUserIdGreaterThanOrderByUserId(@Param("loginName") String loginName, @Param("userId") Long userId);

    List<SysUser> listByLoginNameStartWithOrderByUserId(@Param("loginName") String loginName);

    List<SysUser> listByLoginNameEndWithOrderByUserId(@Param("loginName") String loginName);

    List<SysUser> listByLoginDateIsNullAndPwdUpdateDateIsNullOrderByUserId();

    List<SysUser> listByStatusOrderByUserIdLimitOffsetToRowCount(@Param("status") String status, @Param("offset") int offset, @Param("rowCount") int rowCount);

    List<SysUser> listByStatusOrderByUserIdLimit1To1(@Param("status") String status);

    SysUser getByUserId(SysUser query);

    SysUser getByUserId(@Param("userId") Long userId);

    int updateStatusByParentId(SysUser query);

    int deleteByParentId(@Param("parentId") Long parentId);
}
