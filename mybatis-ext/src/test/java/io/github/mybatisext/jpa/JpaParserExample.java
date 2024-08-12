package io.github.mybatisext.jpa;

import org.apache.ibatis.annotations.Param;

public interface JpaParserExample {

    void select();

    void getDistinctTop10ByUserId$AndTableIdAndRowPrivilegeFieldsDotFieldInXyz$OrderByCreateTime(@Param("xyz") String s);
}
