package io.github.mybatisext.resultmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.LoadType;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.util.StringUtils;

public class ResultMapBuilder {

    private final Configuration originConfiguration;

    public ResultMapBuilder(Configuration originConfiguration) {
        this.originConfiguration = originConfiguration;
    }

    public ResultMap build(TableInfo tableInfo) {
        return build(tableInfo, 1);
    }

    public ResultMap build(TableInfo tableInfo, int nestedLevel) {
        String id = getResultMapId(tableInfo.getTableClass(), tableInfo.getJoinTableInfo().getAlias(), nestedLevel);
        if (originConfiguration.hasResultMap(id)) {
            return originConfiguration.getResultMap(id);
        }

        Class<?> tableClass = tableInfo.getTableClass();
        List<ResultMapping> resultMappings = new ArrayList<>();
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            if (propertyInfo.getColumnInfo() != null) {
                ResultMapping.Builder builder = new ResultMapping.Builder(originConfiguration, propertyInfo.getName())
                        .column(propertyInfo.getColumnInfo().getName())
                        .javaType(propertyInfo.getJavaType())
                        .jdbcType(propertyInfo.getColumnInfo().getJdbcType());
                if (propertyInfo.getIdType() != null) {
                    // id
                    builder.flags(Collections.singletonList(ResultFlag.ID));
                }
                resultMappings.add(builder.build());
            } else {
                if (nestedLevel <= 0) {
                    continue;
                }
                if (LoadType.JOIN.equals(propertyInfo.getLoadType())) {
                    ResultMap resultMap = getNestedResultMap(propertyInfo, nestedLevel - 1);
                    resultMappings.add(new ResultMapping.Builder(originConfiguration, propertyInfo.getName())
                            .javaType(propertyInfo.getJavaType())
                            .nestedResultMapId(resultMap.getId()).build());
                } else {
                    MappedStatement mappedStatement = getNestedSelectStatement(propertyInfo, nestedLevel - 1);
                    ResultMapping.Builder builder = new ResultMapping.Builder(originConfiguration, propertyInfo.getName())
                            .javaType(propertyInfo.getJavaType())
                            .nestedQueryId(mappedStatement.getId());
                    if (LoadType.FETCH_LAZY.equals(propertyInfo.getLoadType())) {
                        builder.lazy(true);
                    } else if (LoadType.FETCH_EAGER.equals(propertyInfo.getLoadType())) {
                        builder.lazy(false);
                    }
                    resultMappings.add(builder.build());
                }
            }
        }

        ResultMap resultMap = new ResultMap.Builder(originConfiguration, id, tableClass, resultMappings).build();
        originConfiguration.addResultMap(resultMap);
        return resultMap;
    }

    private ResultMap getNestedResultMap(PropertyInfo propertyInfo, int nestedLevel) {
        TableInfo tableInfo = propertyInfo.getTableInfo();
        if (StringUtils.isBlank(propertyInfo.getColumnName())) {
            return build(tableInfo, nestedLevel);
        }
        String id = getResultMapId(tableInfo.getTableClass(), tableInfo.getJoinTableInfo().getAlias(), propertyInfo.getColumnName(), nestedLevel);
        if (originConfiguration.hasResultMap(id)) {
            return originConfiguration.getResultMap(id);
        }
        Class<?> javaType;
        if (propertyInfo.getOfType() != null) {
            // collection
            javaType = propertyInfo.getOfType();
        } else {
            // association
            javaType = propertyInfo.getJavaType();
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        resultMappings.add(new ResultMapping.Builder(originConfiguration, propertyInfo.getName())
                .column(propertyInfo.getColumnInfo().getName())
                .javaType(javaType)
                .jdbcType(propertyInfo.getColumnInfo().getJdbcType())
                .build());
        ResultMap resultMap = new ResultMap.Builder(originConfiguration, id, javaType, resultMappings).build();
        originConfiguration.addResultMap(resultMap);
        return resultMap;
    }

    private MappedStatement getNestedSelectStatement(PropertyInfo propertyInfo, int nestedLevel) {
        TableInfo tableInfo = propertyInfo.getTableInfo();
        String id;
        if (StringUtils.isBlank(propertyInfo.getColumnName())) {
            id = getNestedSelectStatementId(tableInfo.getTableClass(), nestedLevel);
        } else {
            id = getNestedSelectStatementId(tableInfo.getTableClass(), propertyInfo.getColumnName(), nestedLevel);
        }
        if (originConfiguration.hasStatement(id)) {
            return originConfiguration.getMappedStatement(id);
        }
        String sql;
        if (StringUtils.isBlank(propertyInfo.getColumnName())) {
            // TODO
            sql = "";
        } else {
            // TODO
            sql = "";
        }
        XMLLanguageDriver xmlLanguageDriver = new XMLLanguageDriver();
        SqlSource sqlSource = xmlLanguageDriver.createSqlSource(originConfiguration, "<script>" + sql + "</script>", Object.class);
        ResultMap resultMap = getNestedResultMap(propertyInfo, nestedLevel);
        MappedStatement mappedStatement = new MappedStatement.Builder(originConfiguration, id, sqlSource, SqlCommandType.SELECT)
                .resultMaps(Collections.singletonList(resultMap))
                .build();
        return mappedStatement;
    }

    private String getResultMapId(Class<?> tableClass, String alias, int nestedLevel) {
        return tableClass.getName() + "@" + alias + "[" + nestedLevel + "]" + "-MybatisExtAutoGenResultMap";
    }

    private String getResultMapId(Class<?> tableClass, String alias, String columnName, int nestedLevel) {
        return tableClass.getName() + "@" + alias + "#" + columnName + "[" + nestedLevel + "]" + "-MybatisExtAutoGenResultMap";
    }

    private String getNestedSelectStatementId(Class<?> tableClass, int nestedLevel) {
        return tableClass.getPackage().getName() + ".select" + tableClass.getSimpleName() + "[" + nestedLevel + "]" + "-MybatisExtAutoGenNestedSelectStatement";
    }

    private String getNestedSelectStatementId(Class<?> tableClass, String columnName, int nestedLevel) {
        return tableClass.getPackage().getName() + ".select" + tableClass.getSimpleName() + "#" + columnName + "[" + nestedLevel + "]" + "-MybatisExtAutoGenNestedSelectStatement";
    }
}
