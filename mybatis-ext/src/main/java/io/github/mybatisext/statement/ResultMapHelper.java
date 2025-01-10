package io.github.mybatisext.statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.LoadType;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.ResultType;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.reflect.GenericType;

public class ResultMapHelper {

    private final Configuration configuration;
    private final MappedStatementHelper mappedStatementHelper;

    public ResultMapHelper(Configuration configuration, MappedStatementHelper mappedStatementHelper) {
        this.configuration = configuration;
        this.mappedStatementHelper = mappedStatementHelper;
    }

    public ResultMap buildResultMap(TableInfo tableInfo, Dialect dialect, boolean changeConfiguration) {
        GenericType tableClass = tableInfo.getTableClass();
        String id = tableClass.getName() + "-Inline";
        if (configuration.hasResultMap(id)) {
            return configuration.getResultMap(id);
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            resultMappings.add(buildResultMapping(id, tableInfo, propertyInfo, dialect, changeConfiguration));
        }
        return new ResultMap.Builder(configuration, id, tableClass.getType(), resultMappings).build();
    }

    public ResultMap buildSimpleTypeResultMap(Class<?> type) {
        return new ResultMap.Builder(configuration, type.getName() + "-Inline", type, new ArrayList<>(0)).build();
    }

    public ResultMap buildOwnResultMap(TableInfo tableInfo, Dialect dialect, boolean changeConfiguration) {
        GenericType tableClass = tableInfo.getTableClass();
        String id = tableClass.getName() + "-Own-Inline";
        if (configuration.hasResultMap(id)) {
            return configuration.getResultMap(id);
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            if (propertyInfo.isOwnColumn()) {
                resultMappings.add(buildResultMapping(id, tableInfo, propertyInfo, dialect, changeConfiguration));
            }
        }
        return new ResultMap.Builder(configuration, id, tableClass.getType(), resultMappings).build();
    }

    private ResultMapping buildResultMapping(String resultMapId, TableInfo tableInfo, PropertyInfo propertyInfo, Dialect dialect, boolean changeConfiguration) {
        if (propertyInfo.getResultType() == ResultType.ID) {
            return new ResultMapping.Builder(configuration, propertyInfo.getName())
                    .column(propertyInfo.getName())
                    .javaType(propertyInfo.getJavaType().getType())
                    .jdbcType(propertyInfo.getJdbcType())
                    .flags(Collections.singletonList(ResultFlag.ID))
                    .build();
        }
        if (propertyInfo.getResultType() == ResultType.RESULT) {
            return new ResultMapping.Builder(configuration, propertyInfo.getName())
                    .column(propertyInfo.getName())
                    .javaType(propertyInfo.getJavaType().getType())
                    .jdbcType(propertyInfo.getJdbcType())
                    .build();
        }
        if (propertyInfo.getResultType() == ResultType.ASSOCIATION) {
            if (propertyInfo.getLoadType() == null || propertyInfo.getLoadType() == LoadType.JOIN) {
                String nestedResultMapId = resultMapId + "[association" + "=" + propertyInfo.getName() + "]";
                addNestedResultMap(nestedResultMapId, tableInfo, propertyInfo, propertyInfo.getJavaType(), dialect, changeConfiguration);
                return new ResultMapping.Builder(configuration, propertyInfo.getName())
                        .javaType(propertyInfo.getJavaType().getType())
                        .nestedResultMapId(nestedResultMapId)
                        .build();
            }
            NestedSelect nestedSelect = NestedSelectHelper.buildNestedSelect(tableInfo, propertyInfo);
            String column = NestedSelectHelper.buildResultMappingColumn(nestedSelect);
            addNestedSelectStatement(nestedSelect, dialect, changeConfiguration);
            ResultMapping.Builder builder = new ResultMapping.Builder(configuration, propertyInfo.getName())
                    .column(column)
                    .composites(parseCompositeColumnName(column))
                    .nestedQueryId(NestedSelectHelper.toString(nestedSelect))
                    .javaType(propertyInfo.getJavaType().getType());
            if (propertyInfo.getLoadType() == LoadType.FETCH_LAZY) {
                builder.lazy(true);
            } else if (propertyInfo.getLoadType() == LoadType.FETCH_EAGER) {
                builder.lazy(false);
            }
            return builder.build();
        }
        if (propertyInfo.getResultType() == ResultType.COLLECTION) {
            if (propertyInfo.getLoadType() == null || propertyInfo.getLoadType() == LoadType.JOIN) {
                String nestedResultMapId = resultMapId + "[collection" + "=" + propertyInfo.getName() + "]";
                addNestedResultMap(nestedResultMapId, tableInfo, propertyInfo, propertyInfo.getOfType(), dialect, changeConfiguration);
                return new ResultMapping.Builder(configuration, propertyInfo.getName())
                        .javaType(propertyInfo.getJavaType().getType())
                        .nestedResultMapId(nestedResultMapId)
                        .build();
            }
            NestedSelect nestedSelect = NestedSelectHelper.buildNestedSelect(tableInfo, propertyInfo);
            String column = NestedSelectHelper.buildResultMappingColumn(nestedSelect);
            addNestedSelectStatement(nestedSelect, dialect, changeConfiguration);
            ResultMapping.Builder builder = new ResultMapping.Builder(configuration, propertyInfo.getName())
                    .column(column)
                    .composites(parseCompositeColumnName(column))
                    .nestedQueryId(NestedSelectHelper.toString(nestedSelect))
                    .javaType(propertyInfo.getJavaType().getType());
            if (propertyInfo.getLoadType() == LoadType.FETCH_LAZY) {
                builder.lazy(true);
            } else if (propertyInfo.getLoadType() == LoadType.FETCH_EAGER) {
                builder.lazy(false);
            }
            return builder.build();
        }
        throw new MybatisExtException("Unknown resultType: " + propertyInfo.getResultType());
    }

    // org.apache.ibatis.builder.MapperBuilderAssistant#parseCompositeColumnName
    private List<ResultMapping> parseCompositeColumnName(String columnName) {
        List<ResultMapping> composites = new ArrayList<>();
        if (columnName != null && (columnName.indexOf('=') > -1 || columnName.indexOf(',') > -1)) {
            StringTokenizer parser = new StringTokenizer(columnName, "{}=, ", false);
            while (parser.hasMoreTokens()) {
                String property = parser.nextToken();
                String column = parser.nextToken();
                ResultMapping complexResultMapping = new ResultMapping.Builder(configuration, property, column, configuration.getTypeHandlerRegistry().getUnknownTypeHandler()).build();
                composites.add(complexResultMapping);
            }
        }
        return composites;
    }

    private void addNestedResultMap(String id, TableInfo tableInfo, PropertyInfo propertyInfo, GenericType propertyType, Dialect dialect, boolean changeConfiguration) {
        if (configuration.hasResultMap(id)) {
            return;
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo subPropertyInfo : propertyInfo.values()) {
            resultMappings.add(buildResultMapping(id, tableInfo, subPropertyInfo, dialect, changeConfiguration));
        }
        ResultMap resultMap = new ResultMap.Builder(configuration, id, propertyType.getType(), resultMappings).build();
        if (changeConfiguration) {
            synchronized (configuration) {
                if (!configuration.hasResultMap(resultMap.getId())) {
                    configuration.addResultMap(resultMap);
                }
            }
        }
    }

    private void addNestedSelectStatement(NestedSelect nestedSelect, Dialect dialect, boolean changeConfiguration) {
        String id = NestedSelectHelper.toString(nestedSelect);
        if (configuration.hasStatement(id)) {
            return;
        }
        MappedStatement ms = mappedStatementHelper.buildForNestedSelect(id, nestedSelect, dialect, changeConfiguration);
        if (changeConfiguration) {
            synchronized (configuration) {
                if (!configuration.hasStatement(ms.getId())) {
                    configuration.addMappedStatement(ms);
                }
            }
        }
    }
}
