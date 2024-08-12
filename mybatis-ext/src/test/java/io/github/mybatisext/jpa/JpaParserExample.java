package io.github.mybatisext.jpa;

import org.apache.ibatis.annotations.Param;

public interface JpaParserExample {

    void select(@Param("tableId") String tableId, @Param("userId") String userId);

    void getDistinctTop10ByUserId$AndTableIdAndRowPrivilegeFieldsDotFieldInXyz$OrderByCreateTime(@Param("xyz") String s);
}
