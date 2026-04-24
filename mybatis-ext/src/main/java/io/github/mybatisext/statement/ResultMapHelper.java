package io.github.mybatisext.statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.FetchType;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.PropertyType;
import io.github.mybatisext.metadata.TableDefFactory;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericType;

public class ResultMapHelper {

    private static final String PREFIX = "__MYBATIS_EXT__";
    private static final Log log = LogFactory.getLog(ResultMapHelper.class);
    private final Configuration configuration;
    private final ExtContext extContext;

    public ResultMapHelper(Configuration configuration, ExtContext extContext) {
        this.configuration = configuration;
        this.extContext = extContext;
    }

    public ResultMap buildResultMap(GenericType returnType, Dialect dialect, boolean writeConfiguration) {
        String id = PREFIX + returnType.getName();
        if (configuration.hasResultMap(id)) {
            return configuration.getResultMap(id);
        }
        if (void.class.equals(returnType.getType()) || configuration.getTypeHandlerRegistry().hasTypeHandler(returnType.getType()) || TableDefFactory.resolveTableType(returnType) == null) {
            return buildSimpleTypeResultMap(returnType.getType());
        }
        TableInfo tableInfo = TableInfoFactory.buildTableInfo(returnType, configuration, extContext);
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            resultMappings.add(buildResultMapping(tableInfo, propertyInfo, dialect, writeConfiguration, false));
        }
        return new ResultMap.Builder(configuration, id, returnType.getType(), resultMappings).build();
    }

    public ResultMap buildSimpleTypeResultMap(Class<?> type) {
        return new ResultMap.Builder(configuration, type.getName() + "-Inline", type, new ArrayList<>(0)).build();
    }

    public ResultMap buildPropertyResultMap(TableInfo tableInfo, PropertyInfo propertyInfo, Dialect dialect, boolean writeConfiguration, boolean inNestedSelect) {
        GenericType tableClass = tableInfo.getClassType();
        String id = PREFIX + tableClass.getName() + "|" + propertyInfo.getName();
        if (configuration.hasResultMap(id)) {
            return configuration.getResultMap(id);
        }
        GenericType propertyType = propertyInfo.getPropertyType() == PropertyType.COLLECTION ? propertyInfo.getOfType() : propertyInfo.getJavaType();
        if (propertyInfo.getColumnName() != null) {
            return buildSimpleTypeResultMap(propertyType.getType());
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo subPropertyInfo : propertyInfo.getNameToPropertyInfo().values()) {
            resultMappings.add(buildResultMapping(tableInfo, subPropertyInfo, dialect, writeConfiguration, inNestedSelect));
        }
        return new ResultMap.Builder(configuration, id, propertyType.getType(), resultMappings).build();
    }

    private ResultMapping buildResultMapping(TableInfo tableInfo, PropertyInfo propertyInfo, Dialect dialect, boolean writeConfiguration, boolean inNestedSelect) {
        if (propertyInfo.getPropertyType() == PropertyType.ID) {
            return new ResultMapping.Builder(configuration, propertyInfo.getName())
                    .column(propertyInfo.getFullName())
                    .javaType(propertyInfo.getJavaType().getType())
                    .jdbcType(propertyInfo.getJdbcType())
                    .flags(Collections.singletonList(ResultFlag.ID))
                    .build();
        }
        if (propertyInfo.getPropertyType() == PropertyType.RESULT) {
            return new ResultMapping.Builder(configuration, propertyInfo.getName())
                    .column(propertyInfo.getFullName())
                    .javaType(propertyInfo.getJavaType().getType())
                    .jdbcType(propertyInfo.getJdbcType())
                    .build();
        }
        if (propertyInfo.getPropertyType() == PropertyType.ASSOCIATION || propertyInfo.getPropertyType() == PropertyType.COLLECTION) {
            // 若已在嵌套查询中，则强制使用嵌套结果映射以避免多层嵌套查询影响性能
            if (inNestedSelect || propertyInfo.getFetchType() == null) {
                ResultMap resultMap = addNestedResultMap(tableInfo, propertyInfo, dialect, writeConfiguration, inNestedSelect);
                return new ResultMapping.Builder(configuration, propertyInfo.getName())
                        .javaType(propertyInfo.getJavaType().getType())
                        .nestedResultMapId(resultMap.getId())
                        .build();
            }
            NestedSelect nestedSelect = NestedSelectHelper.buildNestedSelect(tableInfo, propertyInfo);
            String column = NestedSelectHelper.buildResultMappingColumn(nestedSelect);
            MappedStatement mappedStatement = addNestedSelectStatement(nestedSelect, dialect, writeConfiguration);
            ResultMapping.Builder builder = new ResultMapping.Builder(configuration, propertyInfo.getName())
                    .column(column)
                    .composites(parseCompositeColumnName(column))
                    .nestedQueryId(mappedStatement.getId())
                    .javaType(propertyInfo.getJavaType().getType());
            if (propertyInfo.getFetchType() == FetchType.LAZY) {
                builder.lazy(true);
            } else if (propertyInfo.getFetchType() == FetchType.EAGER) {
                builder.lazy(false);
            }
            return builder.build();
        }
        throw new MybatisExtException("Unknown PropertyType: " + propertyInfo.getPropertyType());
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

    private ResultMap addNestedResultMap(TableInfo tableInfo, PropertyInfo propertyInfo, Dialect dialect, boolean writeConfiguration, boolean inNestedSelect) {
        ResultMap resultMap = buildPropertyResultMap(tableInfo, propertyInfo, dialect, writeConfiguration, inNestedSelect);
        if (writeConfiguration) {
            synchronized (configuration) {
                if (!configuration.hasResultMap(resultMap.getId())) {
                    configuration.addResultMap(resultMap);
                }
            }
        }
        return resultMap;
    }

    private MappedStatement addNestedSelectStatement(NestedSelect nestedSelect, Dialect dialect, boolean writeConfiguration) {
        String id = NestedSelectHelper.toString(nestedSelect);
        if (configuration.hasStatement(id)) {
            return configuration.getMappedStatement(id);
        }
        MappedStatement ms = buildForNestedSelect(id, nestedSelect, dialect, writeConfiguration);
        if (writeConfiguration) {
            synchronized (configuration) {
                if (!configuration.hasStatement(ms.getId())) {
                    configuration.addMappedStatement(ms);
                }
            }
        }
        return ms;
    }

    private MappedStatement buildForNestedSelect(String id, NestedSelect nestedSelect, Dialect dialect, boolean writeConfiguration) {
        log.debug(id);
        List<ResultMap> resultMaps = new ArrayList<>();
        resultMaps.add(buildPropertyResultMap(nestedSelect.getTableInfo(), nestedSelect.getPropertyInfo(), dialect, writeConfiguration, true));
        String script = NestedSelectHelper.buildNestedSelectScript(nestedSelect, dialect);
        log.debug(script);
        SqlSource sqlSource = new XMLLanguageDriver().createSqlSource(configuration, script, Object.class);
        MappedStatement.Builder builder = new MappedStatement.Builder(configuration, id, sqlSource, SqlCommandType.SELECT);
        return builder.resultMaps(resultMaps).resultSetType(ResultSetType.DEFAULT).build();
    }
}
