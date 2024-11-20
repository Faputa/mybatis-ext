package io.github.mybatisext.test.spring;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TestOgnlMapper {
    List<Object> testOgnlInForeach(Object value);

    List<Object> testVaragsToList(Object a, Object b);

    List<Object> testOgnlCallMapperStatement();

    int testBind();

    int testParameterObject1(int a);

    int testParameterObject2(@Param("a") int a);

    int testParameterObject3(@Param("a") int a, @Param("b") int b);

    int testParameterObject4(Org a);

    int testParameterObject5(Map<String, Object> a);

    int testParameterObject6(List<Object> a);

    int testParameterObject7(Set<Object> a);

    int testParameterObject8(Object[] a);
}
