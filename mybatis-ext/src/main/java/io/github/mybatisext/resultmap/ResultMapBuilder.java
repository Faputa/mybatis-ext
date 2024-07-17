package io.github.mybatisext.resultmap;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

public class ResultMapBuilder {

    private final Configuration configuration;

    public ResultMapBuilder(Configuration configuration) {
        this.configuration = configuration;
    }

    public ResultMap buildResultMap(TableInfo tableInfo) {
        Class<?> tableClass = tableInfo.getTableClass();
        String id = tableClass.getName();
        if (configuration.hasResultMap(id)) {
            return configuration.getResultMap(id);
        }

        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            resultMappings.add(propertyInfo.getResultMapping(configuration, id));
        }

        ResultMap resultMap = new ResultMap.Builder(configuration, id, tableClass, resultMappings).build();
        configuration.addResultMap(resultMap);
        return resultMap;
    }

    public void buildNestedResultMap(PropertyInfo propertyInfo, String id) {
        if (configuration.hasResultMap(id)) {
            configuration.getResultMap(id);
            return;
        }

        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo subPropertyInfo : propertyInfo.getSubPropertyInfos()) {
            resultMappings.add(subPropertyInfo.getResultMapping(configuration, id));
        }

        ResultMap resultMap = new ResultMap.Builder(configuration, id, propertyInfo.getJavaType(), resultMappings).build();
        configuration.addResultMap(resultMap);
    }
}
