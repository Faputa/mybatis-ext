package io.github.mybatisext.jpa;

import java.util.List;

import io.github.mybatisext.annotation.OnlyById;
import org.apache.ibatis.annotations.Param;

import io.github.mybatisext.mapper.BaseMapper;
import io.github.mybatisext.metadata.TablePermission;

public interface JpaParserExample extends BaseMapper<TablePermission> {

    TablePermission get(@Param("tableId") String tableId, @Param("roleId") String roleId);

    TablePermission getDataSourceName(@Param("tableId") String tableId, @Param("roleId") String roleId);

    TablePermission getDistinctTop10ByRoleId$AndTableIdAndColumnPermissionsDotColumnNameInXyz$OrderByCreatedAt(@Param("roleId") String roleId, @Param("tableId") String tableId, @Param("xyz") List<String> ss);

    TablePermission getByTableId(TablePermission query);

    TablePermission getByTableId(@Param("tableId") String tableId);

    TablePermission getByTableIdIsTpDotTableId(@Param("tp") TablePermission query);

    TablePermission getByTableIdOrderByIdAndTableId(TablePermission query);

    TablePermission getByTableIdGroupByIdAndTableId(TablePermission query);

    int deleteByDataSourceName(String dbName);

    int deleteByDataSourceName(TablePermission query);

    int updateIgnoreNullByDataSourceName(TablePermission query);

    int updatePermissionType(@OnlyById TablePermission query);

    int updatePermissionTypeAndUpdatedAt(@OnlyById TablePermission query);

    int updatePermissionTypeAndUpdatedAtByRoleId(TablePermission query);

    List<TablePermission> findAll();
}
