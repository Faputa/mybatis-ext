package io.github.mybatisext.statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.LoadType;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.ResultType;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.reflect.GenericType;

public class ResultMapHelper {

    public static ResultMap buildResultMap(Configuration configuration, TableInfo tableInfo) {
        GenericType tableClass = tableInfo.getTableClass();
        String id = tableClass.getName() + "-Inline";
        if (configuration.hasResultMap(id)) {
            return configuration.getResultMap(id);
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            resultMappings.add(buildResultMapping(configuration, id, propertyInfo));
        }
        return new ResultMap.Builder(configuration, id, tableClass.getType(), resultMappings).build();
    }

    public static ResultMap buildSimpleTypeResultMap(Configuration configuration, Class<?> type) {
        return new ResultMap.Builder(configuration, type.getName() + "-Inline", type, new ArrayList<>(0)).build();
    }

    public static ResultMap buildOwnResultMap(Configuration configuration, TableInfo tableInfo) {
        GenericType tableClass = tableInfo.getTableClass();
        String id = tableClass.getName() + "-Own-Inline";
        if (configuration.hasResultMap(id)) {
            return configuration.getResultMap(id);
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            if (propertyInfo.isOwnColumn()) {
                resultMappings.add(buildResultMapping(configuration, id, propertyInfo));
            }
        }
        return new ResultMap.Builder(configuration, id, tableClass.getType(), resultMappings).build();
    }

    private static ResultMapping buildResultMapping(Configuration configuration, String resultMapId, PropertyInfo propertyInfo) {
        if (propertyInfo.getResultType() == ResultType.ID) {
            return new ResultMapping.Builder(configuration, propertyInfo.getName())
                    .column(propertyInfo.getJoinTableInfo().getAlias() + "_" + propertyInfo.getColumnName())
                    .javaType(propertyInfo.getJavaType().getType())
                    .jdbcType(propertyInfo.getJdbcType())
                    .flags(Collections.singletonList(ResultFlag.ID))
                    .build();
        }
        if (propertyInfo.getResultType() == ResultType.RESULT) {
            return new ResultMapping.Builder(configuration, propertyInfo.getName())
                    .column(propertyInfo.getJoinTableInfo().getAlias() + "_" + propertyInfo.getColumnName())
                    .javaType(propertyInfo.getJavaType().getType())
                    .jdbcType(propertyInfo.getJdbcType())
                    .build();
        }
        if (propertyInfo.getResultType() == ResultType.ASSOCIATION) {
            if (propertyInfo.getLoadType() == null || propertyInfo.getLoadType() == LoadType.JOIN) {
                String nestedResultMapId = resultMapId + "[association" + "=" + propertyInfo.getName() + "]";
                ResultMapHelper.addNestedResultMap(configuration, nestedResultMapId, propertyInfo);
                return new ResultMapping.Builder(configuration, propertyInfo.getName())
                        .javaType(propertyInfo.getJavaType().getType())
                        .nestedResultMapId(nestedResultMapId)
                        .build();
            }
            NestedSelect nestedSelect = NestedSelectHelper.buildNestedSelect(propertyInfo);
            return new ResultMapping.Builder(configuration, propertyInfo.getName())
                    .column(NestedSelectHelper.buildResultMappingColumn(nestedSelect))
                    .nestedQueryId(NestedSelectHelper.toString(nestedSelect))
                    .javaType(propertyInfo.getJavaType().getType())
                    .build();
        }
        if (propertyInfo.getResultType() == ResultType.COLLECTION) {
            if (propertyInfo.getLoadType() == null || propertyInfo.getLoadType() == LoadType.JOIN) {
                String nestedResultMapId = resultMapId + "[collection" + "=" + propertyInfo.getName() + "]";
                ResultMapHelper.addNestedResultMap(configuration, nestedResultMapId, propertyInfo);
                return new ResultMapping.Builder(configuration, propertyInfo.getName())
                        .javaType(propertyInfo.getOfType().getType())
                        .nestedResultMapId(nestedResultMapId)
                        .build();
            }
            NestedSelect nestedSelect = NestedSelectHelper.buildNestedSelect(propertyInfo);
            return new ResultMapping.Builder(configuration, propertyInfo.getName())
                    .column(NestedSelectHelper.buildResultMappingColumn(nestedSelect))
                    .nestedQueryId(NestedSelectHelper.toString(nestedSelect))
                    .javaType(propertyInfo.getOfType().getType())
                    .build();
        }
        throw new MybatisExtException("Unknown resultType: " + propertyInfo.getResultType());
    }

    private static void addNestedResultMap(Configuration configuration, String id, PropertyInfo propertyInfo) {
        if (configuration.hasResultMap(id)) {
            return;
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo subPropertyInfo : propertyInfo.values()) {
            resultMappings.add(buildResultMapping(configuration, id, subPropertyInfo));
        }
        ResultMap resultMap = new ResultMap.Builder(configuration, id, propertyInfo.getJavaType().getType(), resultMappings).build();
        synchronized (configuration) {
            if (!configuration.hasResultMap(id)) {
                configuration.addResultMap(resultMap);
            }
        }
    }
}
