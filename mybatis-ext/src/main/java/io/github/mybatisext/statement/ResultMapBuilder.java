package io.github.mybatisext.statement;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.reflect.GenericType;

public class ResultMapBuilder {

    public static ResultMap buildResultMap(Configuration configuration, TableInfo tableInfo) {
        GenericType tableClass = tableInfo.getTableClass();
        String id = tableClass.getName() + "-Inline";
        if (configuration.hasResultMap(id)) {
            return configuration.getResultMap(id);
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            resultMappings.add(propertyInfo.getResultMapping(configuration, id));
        }
        return new ResultMap.Builder(configuration, id, tableClass.getType(), resultMappings).build();
    }

    public static void addNestedResultMap(Configuration configuration, String id, PropertyInfo propertyInfo) {
        if (configuration.hasResultMap(id)) {
            return;
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo subPropertyInfo : propertyInfo.values()) {
            resultMappings.add(subPropertyInfo.getResultMapping(configuration, id));
        }
        ResultMap resultMap = new ResultMap.Builder(configuration, id, propertyInfo.getJavaType().getType(), resultMappings).build();
        synchronized (ResultMapBuilder.class) {
            if (!configuration.hasResultMap(id)) {
                configuration.addResultMap(resultMap);
            }
        }
    }
}
