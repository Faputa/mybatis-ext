package io.github.mybatisext.test;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import io.github.mybatisext.annotation.OnlyById;
import io.github.mybatisext.mapper.BaseMapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    List<SysUser> list(SysUserVO sysUserVO);

    List<SysUserVO> listSysUserVO(SysUser sysUser);

    SysUser getByLoginName(@Param("loginName") String loginName);

    long countByStatus(@Param("status") String status);

    long count(@Param("status") String status);

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

    SysUser get(@Param("userId") Long userId);

    int updateStatusByParentId(SysUser query);

    int deleteByParentId(@Param("parentId") Long parentId);

    // 查询入口、投影与排序。
    List<SysUser> findAllOrderByUserId();

    List<SysUser> selectAllOrderByUserId();

    SysUser getOneByStatusOrderByUserIdDesc(@Param("status") String status);

    List<String> listDistinctStatusOrderByStatus();

    List<SysUser> listUserIdAndLoginNameOrderByUserId();

    List<SysUser> listOrderByStatusAscAndUserIdDesc();

    List<SysUser> listByUserIdGreaterThanOrderByUserId(RowBounds bounds, long value);

    List<SysUser> findWhereLoginNameEquals(@Param("value") String value);

    List<SysUser> selectByLoginNameIs(@Param("value") String value);

    // 比较、否定、大小写、集合与空值运算。
    List<SysUser> listByUserIdLessThanOrderByUserId(long value);

    List<SysUser> listByUserIdLessThanEqualOrderByUserId(long value);

    List<SysUser> listByUserIdGreaterThanEqualOrderByUserId(long value);

    List<SysUser> listByUserIdNotGreaterThanOrderByUserId(long value);

    List<SysUser> listByLoginNameNotOrderByUserId(String value);

    List<SysUser> listByLoginNameIgnorecaseEqualsOrderByUserId(String value);

    List<SysUser> listByLoginNameLikeOrderByUserId(String value);

    List<SysUser> listByLoginNameNotLikeOrderByUserId(String value);

    List<SysUser> listByLoginNameIgnorecaseLikeOrderByUserId(String value);

    List<SysUser> listByLoginNameIgnorecaseStartWithOrderByUserId(String value);

    List<SysUser> listByLoginNameIgnorecaseEndWithOrderByUserId(String value);

    List<SysUser> listByLoginNameIgnorecaseNotLikeOrderByUserId(String value);

    List<SysUser> listByUserIdBetweenOrderByUserId(long lower, long upper);

    List<SysUser> listByUserIdNotBetweenOrderByUserId(long lower, long upper);

    List<SysUser> listByUserIdNotInOrderByUserId(@Param("ids") List<Long> ids);

    List<SysUser> listByLoginNameIgnorecaseInOrderByUserId(@Param("names") List<String> names);

    List<SysUser> listByEmailIsNullOrderByUserId();

    List<SysUser> listByEmailIsNotNullOrderByUserId();

    List<SysUser> listByEmailNotIsNullOrderByUserId();

    List<SysUser> listByEmailNotIsNotNullOrderByUserId();

    // 逻辑优先级、重复条件与显式、顺序参数绑定。
    List<SysUser> listByUserIdOrUserIdAndStatusOrderByUserId(long first, long second, String state);

    List<SysUser> listByUserIdAndStatusOrUserIdOrderByUserId(long first, String state, long second);

    List<SysUser> listByUserIdOrUserIdOrUserIdOrderByUserId(long first, long second, long third);

    List<SysUser> listByUserIdIsChosenOrUserIdIsChosenOrderByUserId(@Param("chosen") long chosen);

    List<SysUser> listByUserIdBetweenOrLoginNameOrderByUserId(long lower, long upper, String name);

    List<SysUser> listByUserIdOrLoginNameIsPatternOrderByUserId(@Param("pattern") String pattern, @Param("criterion") long criterion);

    List<SysUser> listByUserIdIsCriteriaDotUserIdOrderByUserId(@Param("criteria") SysUser criteria);

    List<SysUser> listByDeptDotDeptIdIsCriteriaDotDeptIdOrderByUserId(@Param("criteria") SysUser criteria);

    List<SysUser> listByDeptDotDeptNameOrderByUserId(@Param("name") String name);

    List<SysUser> listByLoginNameIsValue$AndStatusIsState$OrderByUserId(@Param("value") String value, @Param("state") String state);

    // Top、One 与各类 LIMIT 参数组合。
    List<SysUser> listTopCountByUserIdGreaterThanOrderByUserId(@Param("count") int count, @Param("criterion") long criterion);

    List<SysUser> listOrderByUserIdLimit2();

    List<SysUser> listOrderByUserIdLimitCount(@Param("count") int count);

    List<SysUser> listOrderByUserIdLimit1To2();

    List<SysUser> listOrderByUserIdLimitOffsetToCount(@Param("offset") int offset, @Param("count") int count);

    List<SysUser> listOrderByUserIdLimit1ToCount(@Param("count") int count);

    List<SysUser> listOrderByUserIdLimitOffsetTo1(@Param("offset") int offset);

    List<SysUser> listTop1OrderByUserIdLimit2();

    List<SysUser> listOneOrderByUserIdLimitCount(@Param("count") int count);

    // 分组、显式 HAVING、默认 HAVING 与跨子句参数预留。
    List<String> listStatusGroupByStatusOrderByStatus();

    List<String> findStatusGroupByStatusOrderByStatus(@Param("status") String status);

    List<String> listStatusByUserIdGreaterThanGroupByStatusHavingStatusIsTargetOrderByStatusLimitCount(
            @Param("target") String target, @Param("count") int count, @Param("criterion") long criterion);

    List<Long> listUserIdByUserIdGreaterThanGroupByUserIdHavingUserIdLessThanOrderByUserId(long lower, long upper);

    List<Long> listUserIdByUserIdOrUserIdGroupByUserIdHavingUserIdGreaterThanThresholdOrderByUserIdLimitCount(
            @Param("threshold") long threshold, @Param("count") int count, @Param("first") long first, @Param("second") long second);

    List<Long> listUserIdByUserIdGroupByUserId(SysUser criteria);

    List<SysUserStatusVO> listDeptIdAndStatusGroupByDeptIdAndStatusOrderByDeptIdAscAndStatusDesc();

    List<Long> listUserIdGroupByUserIdHavingUserIdLessThanOrUserIdGreaterThanAndUserIdLessThanOrderByUserId(long upper, long lower, long maximum);

    boolean existsWhereUserIdGreaterThan(long value);

    long countWhereUserIdGreaterThan(long value);

    // CRUD 别名、显式更新字段与批量形式。
    int insert(SysUser user);

    int insertIgnoreNull(SysUser user);

    int insertBatchIgnoreNull(List<SysUser> users);

    int modifyLoginNameAndEmailByUserId(SysUser user);

    int modifyLoginNameAndEmailIgnoreNullWhereUserId(SysUser user);

    int modifyBatchStatusIgnoreNullByUserId(List<SysUser> users);

    int updateBatchStatusIgnoreNullByUserId(@Param("users") List<SysUser> users);

    int modifyBatchStatusIgnoreNullWhereUserId(SysUser[] users);

    int removeWhereUserId(@Param("id") long id);

    int removeBatch(@OnlyById List<SysUser> users);

    int removeBatchByUserId(@Param("users") List<SysUser> users);
}
