package io.github.mybatisext.statement;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

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
    private final AutoMethodStatementBuilder autoMethodStatementBuilder = new AutoMethodStatementBuilder();

    public MappedStatementBuilder(Configuration originConfiguration, ExtContext extContext) {
        this.originConfiguration = originConfiguration;
    }

    public MappedStatement build(String id, String methodName, List<Method> methods, Class<?> returnType, Class<?> tableType) {
        MappedStatement ms = autoMethodStatementBuilder.build(id, methodName, methods, returnType, tableType);
        DataSource dataSource = originConfiguration.getEnvironment().getDataSource();
        // TODO 可以根据dataSource选择方言
        try (Connection connection = dataSource.getConnection()) {
            // TODO 注意如果是动态数据源dataSource可能会变化
            String url = connection.getMetaData().getURL();
            Map<String, String> m = Arrays.stream(url.split("\\?")[1].split("\\&")).map(v -> v.split("=")).collect(Collectors.toMap(v -> v[0], v -> v[1]));
            System.out.println(url);
            System.out.println(m.get("name"));
        } catch (Exception e) {
        }
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
        ResultMap inlineResultMap = new ResultMap.Builder(originConfiguration, id + "-Inline", Long.class, new ArrayList<>()).build();
        resultMaps.add(inlineResultMap);
        Builder builder = new MappedStatement.Builder(originConfiguration, id, sqlSource, SqlCommandType.SELECT);
        return builder.resultMaps(resultMaps).resultSetType(ResultSetType.DEFAULT).build();
    }
}
