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
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericType;

public class ResultMapHelper {

    public static final String PREFIX = "__MYBATIS_EXT__";

    private final Configuration configuration;
    private final MappedStatementHelper mappedStatementHelper;

    public ResultMapHelper(Configuration configuration, MappedStatementHelper mappedStatementHelper) {
        this.configuration = configuration;
        this.mappedStatementHelper = mappedStatementHelper;
    }

    public ResultMap buildResultMap(GenericType returnType, Dialect dialect, boolean changeConfiguration) {
        String id = PREFIX + returnType.getName();
        if (configuration.hasResultMap(id)) {
            return configuration.getResultMap(id);
        }
        if (configuration.getTypeHandlerRegistry().hasTypeHandler(returnType.getType())) {
            return buildSimpleTypeResultMap(returnType.getType());
        }
        TableInfo tableInfo = TableInfoFactory.getTableInfo(configuration, returnType);
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            resultMappings.add(buildResultMapping(tableInfo, propertyInfo, dialect, changeConfiguration));
        }
        return new ResultMap.Builder(configuration, id, returnType.getType(), resultMappings).build();
    }

    public ResultMap buildSimpleTypeResultMap(Class<?> type) {
        return new ResultMap.Builder(configuration, type.getName() + "-Inline", type, new ArrayList<>(0)).build();
    }

    public ResultMap buildPropertyResultMap(TableInfo tableInfo, PropertyInfo propertyInfo, Dialect dialect, boolean changeConfiguration) {
        GenericType tableClass = tableInfo.getTableClass();
        String id = PREFIX + tableClass.getName() + "|" + propertyInfo.getName();
        if (configuration.hasResultMap(id)) {
            return configuration.getResultMap(id);
        }
        GenericType propertyType = propertyInfo.getResultType() == ResultType.COLLECTION ? propertyInfo.getOfType() : propertyInfo.getJavaType();
        if (propertyInfo.getColumnName() != null) {
            return buildSimpleTypeResultMap(propertyType.getType());
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo subPropertyInfo : propertyInfo.values()) {
            resultMappings.add(buildResultMapping(tableInfo, subPropertyInfo, dialect, changeConfiguration));
        }
        return new ResultMap.Builder(configuration, id, propertyType.getType(), resultMappings).build();
    }

    private ResultMapping buildResultMapping(TableInfo tableInfo, PropertyInfo propertyInfo, Dialect dialect, boolean changeConfiguration) {
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
        if (propertyInfo.getResultType() == ResultType.ASSOCIATION || propertyInfo.getResultType() == ResultType.COLLECTION) {
            if (propertyInfo.getLoadType() == null || propertyInfo.getLoadType() == LoadType.JOIN) {
                ResultMap resultMap = addNestedResultMap(tableInfo, propertyInfo, dialect, changeConfiguration);
                return new ResultMapping.Builder(configuration, propertyInfo.getName())
                        .javaType(propertyInfo.getJavaType().getType())
                        .nestedResultMapId(resultMap.getId())
                        .build();
            }
            NestedSelect nestedSelect = NestedSelectHelper.buildNestedSelect(tableInfo, propertyInfo);
            String column = NestedSelectHelper.buildResultMappingColumn(nestedSelect);
            MappedStatement mappedStatement = addNestedSelectStatement(nestedSelect, dialect, changeConfiguration);
            ResultMapping.Builder builder = new ResultMapping.Builder(configuration, propertyInfo.getName())
                    .column(column)
                    .composites(parseCompositeColumnName(column))
                    .nestedQueryId(mappedStatement.getId())
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

    private ResultMap addNestedResultMap(TableInfo tableInfo, PropertyInfo propertyInfo, Dialect dialect, boolean changeConfiguration) {
        ResultMap resultMap = buildPropertyResultMap(tableInfo, propertyInfo, dialect, changeConfiguration);
        if (changeConfiguration) {
            synchronized (configuration) {
                if (!configuration.hasResultMap(resultMap.getId())) {
                    configuration.addResultMap(resultMap);
                }
            }
        }
        return resultMap;
    }

    private MappedStatement addNestedSelectStatement(NestedSelect nestedSelect, Dialect dialect, boolean changeConfiguration) {
        String id = NestedSelectHelper.toString(nestedSelect);
        if (configuration.hasStatement(id)) {
            return configuration.getMappedStatement(id);
        }
        MappedStatement ms = mappedStatementHelper.buildForNestedSelect(id, nestedSelect, dialect, changeConfiguration);
        if (changeConfiguration) {
            synchronized (configuration) {
                if (!configuration.hasStatement(ms.getId())) {
                    configuration.addMappedStatement(ms);
                }
            }
        }
        return ms;
    }
}
