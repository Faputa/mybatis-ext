package io.github.mybatisext.jpa;

import io.github.mybatisext.table.PrivilegeTable;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface JpaParserExample {

    PrivilegeTable select(@Param("tableId") String tableId, @Param("userId") String userId);

    PrivilegeTable getDistinctTop10ByUserId$AndTableIdAndRowPrivilegeFieldsDotFieldInXyz$OrderByCreateTime(@Param("userId") String userId, @Param("tableId") String tableId, @Param("xyz") String s);

    int save(PrivilegeTable entity);

    int saveBatch(List<PrivilegeTable> list);

    int update(PrivilegeTable entity);

    int updateIgnoreNull(PrivilegeTable entity);

    int delete(PrivilegeTable query);

    int deleteBatch(List<PrivilegeTable> query);

    PrivilegeTable get(PrivilegeTable query);

    PrivilegeTable getByTableId(PrivilegeTable query);

    PrivilegeTable getByTableIdIsPtDotTableId(@Param("pt") PrivilegeTable query);

    List<PrivilegeTable> list(PrivilegeTable query);

    long count(PrivilegeTable query);

    boolean exists(PrivilegeTable query);
}
