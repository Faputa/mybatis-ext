package io.github.mybatisext.resultmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

public class ResultMapBuilder {

    private final Configuration originConfiguration;
    private final Map<TableInfo, ResultMap> resultMapCache = new ConcurrentHashMap<>();

    public ResultMapBuilder(Configuration originConfiguration) {
        this.originConfiguration = originConfiguration;
    }

    public ResultMap build(TableInfo tableInfo) {
        if (resultMapCache.containsKey(tableInfo)) {
            return resultMapCache.get(tableInfo);
        }

        String id = tableInfo.getClass() + "-MybatisExtAutoGenResultMap";
        if (originConfiguration.hasResultMap(id)) {
            return originConfiguration.getResultMap(id);
        }

        Class<?> tableClass = tableInfo.getTableClass();
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            if (ResultType.ID == propertyInfo.getResultType()) {
            } else if (ResultType.RESULT == propertyInfo.getResultType()) {
            } else if (ResultType.ASSOCIATION == propertyInfo.getResultType()) {
            } else if (ResultType.COLLECTION == propertyInfo.getResultType()) {
            } else {
                assert false;
            }
        }
        ResultMap resultMap = new ResultMap.Builder(originConfiguration, id, tableClass, resultMappings).build();
        return resultMap;
    }
}
