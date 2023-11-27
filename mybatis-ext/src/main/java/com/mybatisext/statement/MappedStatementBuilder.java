package com.mybatisext.statement;

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

public class MappedStatementBuilder {

    private final Configuration originConfiguration;

    public MappedStatementBuilder(Configuration originConfiguration) {
        this.originConfiguration = originConfiguration;
    }

    public MappedStatement build(String id, String methodName, List<Method> methods, Class<?> returnType, Class<?> tableType) {
        if (!"countDidaTask2".equals(methodName)) {
            return null;
        }
        // TODO
        // 考虑自己实现SqlSource
        XMLLanguageDriver xmlLanguageDriver = new XMLLanguageDriver();
        SqlSource sqlSource = xmlLanguageDriver.createSqlSource(originConfiguration, "<script>select count(*) from org</script>", Object.class);
        List<ResultMap> resultMaps = new ArrayList<>();
        ResultMap inlineResultMap = new ResultMap.Builder(originConfiguration, id + "-Inline", Integer.class, new ArrayList<>(), null).build();
        resultMaps.add(inlineResultMap);
        Builder builder = new MappedStatement.Builder(originConfiguration, id, sqlSource, SqlCommandType.SELECT);
        return builder.resultMaps(resultMaps).resultSetType(ResultSetType.DEFAULT).build();
    }
}
