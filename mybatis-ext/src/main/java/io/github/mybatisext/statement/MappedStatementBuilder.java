package io.github.mybatisext.statement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.ExtContext;

public class MappedStatementBuilder {

    private final Configuration originConfiguration;
    private final NamedStatementBuilder namedStatementBuilder = new NamedStatementBuilder();

    public MappedStatementBuilder(Configuration originConfiguration, ExtContext extContext) {
        this.originConfiguration = originConfiguration;
    }

    public MappedStatement build(String id, String methodName, List<Method> methods, Class<?> returnType, Class<?> tableType) {
        MappedStatement ms = namedStatementBuilder.build(id, methodName, methods, returnType, tableType);
        if (ms != null) {
            return ms;
        }
        // TODO
        if (!"countCamera2".equals(methodName)) {
            return null;
        }
        // 考虑自己实现SqlSource
        XMLLanguageDriver xmlLanguageDriver = new XMLLanguageDriver();
        SqlSource sqlSource = xmlLanguageDriver.createSqlSource(originConfiguration, "<script>select count(*) from camera</script>", Object.class);
        List<ResultMap> resultMaps = new ArrayList<>();
        ResultMap inlineResultMap = new ResultMap.Builder(originConfiguration, id + "-Inline", Long.class, new ArrayList<>(), null).build();
        resultMaps.add(inlineResultMap);
        Builder builder = new MappedStatement.Builder(originConfiguration, id, sqlSource, SqlCommandType.SELECT);
        return builder.resultMaps(resultMaps).resultSetType(ResultSetType.DEFAULT).build();
    }
}
