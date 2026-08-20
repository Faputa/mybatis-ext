package io.github.mybatisext.test.spring;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OgnlTestMapper {

    List<Object> expandObjectForForeach(Object value);

    List<Object> convertVarargsToList(Object a, Object b);

    List<Object> invokeMappedStatementFromOgnl();

    int evaluateRepeatedBindings();

    int captureRawScalarParameter(int a);

    int captureNamedScalarParameter(@Param("a") int a);

    int captureMultipleNamedParameters(@Param("a") int a, @Param("b") int b);

    int captureBeanParameter(SysDept a);

    int captureMapParameter(Map<String, Object> a);

    int captureListParameter(List<Object> a);

    int captureSetParameter(Set<Object> a);

    int captureArrayParameter(Object[] a);
}
