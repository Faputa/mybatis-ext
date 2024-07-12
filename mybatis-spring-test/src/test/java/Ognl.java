import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import io.github.mybatisext.test.spring.TestOgnlTest;

public class Ognl {
    public static List<Object> objToList(Object obj) {
        List<Object> list = new ArrayList<>();
        list.add(obj);
        list.add(obj);
        return list;
    }

    public static List<Object> varagsToList(Object... obj) {
        return java.util.Arrays.asList(obj);
    }

    public static List<Object> callMapperStatement(String statement, Object... parameter) {
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(TestOgnlTest.configuration.get());
        // 考虑将SqlSession放入线程变量中供线程内复用
        SqlSession session = sqlSessionFactory.openSession();
        HashMap<String, Object> parameterMap = new HashMap<>();
        for (int i = 0; i < parameter.length; i++) {
            parameterMap.put("param" + (i + 1), parameter[i]);
        }
        return session.selectList(statement, parameterMap);
    }
}
