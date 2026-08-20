package io.github.mybatisext.test.spring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class OgnlTestFunctions {
    public static List<Object> objectToList(Object obj) {
        List<Object> list = new ArrayList<>();
        list.add(obj);
        list.add(obj);
        return list;
    }

    public static List<Object> varargsToList(Object... obj) {
        return java.util.Arrays.asList(obj);
    }

    public static List<Object> callMapperStatement(String statement, Object... parameter) {
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(OgnlMapperIntegrationTest.configuration.get());
        // 考虑将SqlSession放入线程变量中供线程内复用
        HashMap<String, Object> parameterMap = new HashMap<>();
        for (int i = 0; i < parameter.length; i++) {
            parameterMap.put("param" + (i + 1), parameter[i]);
        }
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList(statement, parameterMap);
        }
    }

    public static int captureParameterObject(Object parameterObject) {
        OgnlMapperIntegrationTest.parameterObjects.get().add(parameterObject);
        return 1;
    }
}
