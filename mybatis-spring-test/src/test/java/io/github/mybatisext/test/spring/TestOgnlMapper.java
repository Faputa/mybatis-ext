package io.github.mybatisext.test.spring;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestOgnlMapper {
    List<Object> testOgnlInForeach(Object value);

    List<Object> testVaragsToList(Object a, Object b);

	List<Object> testOgnlCallMapperStatement();

    int testBind();
}
