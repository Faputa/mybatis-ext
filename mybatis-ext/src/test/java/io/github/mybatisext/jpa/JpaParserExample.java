package io.github.mybatisext.jpa;

import org.apache.ibatis.annotations.Param;

import io.github.mybatisext.mapper.BaseMapper;
import io.github.mybatisext.table.PrivilegeTable;

public interface JpaParserExample extends BaseMapper<PrivilegeTable> {

    PrivilegeTable get(@Param("tableId") String tableId, @Param("userId") String userId);

    PrivilegeTable getDistinctTop10ByUserId$AndTableIdAndRowPrivilegeFieldsDotFieldInXyz$OrderByCreateTime(@Param("userId") String userId, @Param("tableId") String tableId, @Param("xyz") String s);

    PrivilegeTable getByTableId(PrivilegeTable query);

    PrivilegeTable getByTableId(@Param("tableId") String tableId);

    PrivilegeTable getByTableIdIsPtDotTableId(@Param("pt") PrivilegeTable query);

    PrivilegeTable getByTableIdOrderByIdAndTableId(PrivilegeTable query);
}
