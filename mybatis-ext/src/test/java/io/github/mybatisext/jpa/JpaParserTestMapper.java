package io.github.mybatisext.jpa;

import java.util.List;
import java.sql.Timestamp;

import io.github.mybatisext.annotation.OnlyById;
import org.apache.ibatis.annotations.Param;

import io.github.mybatisext.mapper.BaseMapper;
import io.github.mybatisext.fixture.permission.TablePermission;

public interface JpaParserTestMapper extends BaseMapper<TablePermission> {

    TablePermission get(@Param("tableId") String tableId, @Param("roleId") String roleId);

    TablePermission getDataSourceName(@Param("tableId") String tableId, @Param("roleId") String roleId);

    TablePermission getDistinctTop10ByRoleId$AndTableIdAndColumnPermissionsDotColumnNameInXyz$OrderByCreatedAt(@Param("roleId") String roleId, @Param("tableId") String tableId, @Param("xyz") List<String> ss);

    TablePermission getByTableId(TablePermission query);

    TablePermission getByTableId(@Param("tableId") String tableId);

    TablePermission getByTableIdIsTpDotTableId(@Param("tp") TablePermission query);

    TablePermission getByTableIdOrderByIdAndTableId(TablePermission query);

    TablePermission getByTableIdGroupByIdAndTableId(TablePermission query);

    TablePermission getTablePermissionByTableIdGroupByIdAndTableId(TablePermission query);

    int deleteByDataSourceName(String dbName);

    int deleteByDataSourceName(TablePermission query);

    int updateIgnoreNullByDataSourceName(TablePermission query);

    int updatePermissionType(@OnlyById TablePermission query);

    int updatePermissionTypeAndUpdatedAt(@OnlyById TablePermission query);

    int updatePermissionTypeAndUpdatedAtByRoleId(TablePermission query);

    List<TablePermission> listByPermissionTypeIgnorecaseLike(@Param("permissionType") String permissionType);

    List<TablePermission> listByPermissionTypeStartWith(@Param("permissionType") String permissionType);

    List<TablePermission> listByPermissionTypeEndWith(@Param("permissionType") String permissionType);

    List<TablePermission> listByCreatedAtBetweenStartToEnd(@Param("start") Timestamp start, @Param("end") Timestamp end);

    List<TablePermission> listByCreatedAtLessThan(@Param("createdAt") Timestamp createdAt);

    List<TablePermission> listByCreatedAtLessThanEqual(@Param("createdAt") Timestamp createdAt);

    List<TablePermission> listByCreatedAtGreaterThan(@Param("createdAt") Timestamp createdAt);

    List<TablePermission> listByCreatedAtGreaterThanEqual(@Param("createdAt") Timestamp createdAt);

    List<TablePermission> listByUpdatedAtIsNullOrCreatedAtIsNotNull();

    List<TablePermission> listByEnabledIsTrue();

    List<TablePermission> listByEnabledIsFalse();

    List<TablePermission> listOrderByIdLimitOffsetToRowCount(@Param("offset") int offset, @Param("rowCount") int rowCount);

    List<TablePermission> listOrderByIdLimit2();

    List<TablePermission> listOrderByIdLimit2To3();

    int deleteByTableIdIn(@Param("tableId") List<String> tableIds);

    int deleteByRoleId$AndTableIdNotIn(@Param("roleId") String roleId, @Param("tableId") List<String> tableIds);

    List<TablePermission> findAll();

    List<TablePermission> findAllTablePermission();
}
