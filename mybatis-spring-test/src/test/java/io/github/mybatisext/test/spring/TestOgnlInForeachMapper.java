package io.github.mybatisext.test.spring;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestOgnlInForeachMapper {
    List<String> testOgnlInForeach(String value);
}
