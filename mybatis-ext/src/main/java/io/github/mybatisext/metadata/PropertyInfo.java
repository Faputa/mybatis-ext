package io.github.mybatisext.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.idgenerator.IdGenerator;
import io.github.mybatisext.resultmap.ResultMapBuilder;
import io.github.mybatisext.resultmap.ResultType;
import io.github.mybatisext.util.StringUtils;

public class PropertyInfo {

    private String name;
    // 如果是简单类型属性
    private String columnName;
    private TableInfo tableInfo;
    private JoinTableInfo joinTableInfo;
    private Class<?> javaType;
    private JdbcType jdbcType;

    // resultMap项的类型
    private ResultType resultType;

    // resultType=ID
    private IdType idType;
    private IdGenerator<?> customIdGenerator;

    // resultType=COLLECTION
    private Class<?> ofType;

    // resultType=ASSOCIATION,COLLECTION且为非关联表属性
    private final List<PropertyInfo> subPropertyInfos = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public void setTableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    public JoinTableInfo getJoinTableInfo() {
        return joinTableInfo;
    }

    public void setJoinTableInfo(JoinTableInfo joinTableInfo) {
        this.joinTableInfo = joinTableInfo;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public void setJavaType(Class<?> javaType) {
        this.javaType = javaType;
    }

    public JdbcType getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(JdbcType jdbcType) {
        this.jdbcType = jdbcType;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public void setResultType(ResultType resultType) {
        this.resultType = resultType;
    }

    public IdType getIdType() {
        return idType;
    }

    public void setIdType(IdType idType) {
        this.idType = idType;
    }

    public IdGenerator<?> getCustomIdGenerator() {
        return customIdGenerator;
    }

    public void setCustomIdGenerator(IdGenerator<?> customIdGenerator) {
        this.customIdGenerator = customIdGenerator;
    }

    public Class<?> getOfType() {
        return ofType;
    }

    public void setOfType(Class<?> ofType) {
        this.ofType = ofType;
    }

    public List<PropertyInfo> getSubPropertyInfos() {
        if (ResultType.ASSOCIATION == resultType || ResultType.COLLECTION == resultType) {
            if (joinTableInfo.getTableInfo() == tableInfo) {
                return subPropertyInfos;
            }
            if (StringUtils.isNotBlank(columnName)) {
                PropertyInfo newPropertyInfo = new PropertyInfo();
                newPropertyInfo.setColumnName(columnName);
                newPropertyInfo.setJavaType(ResultType.COLLECTION == resultType ? ofType : javaType);
                newPropertyInfo.setJdbcType(jdbcType);
                newPropertyInfo.setResultType(ResultType.RESULT);
                return Collections.singletonList(newPropertyInfo);
            }
            return joinTableInfo.getTableInfo().getNameToPropertyInfo().values().stream().filter(propertyInfo -> ResultType.ID == propertyInfo.getResultType() || ResultType.RESULT == propertyInfo.getResultType()).map(propertyInfo -> {
                PropertyInfo newPropertyInfo = new PropertyInfo();
                newPropertyInfo.setName(propertyInfo.getName());
                newPropertyInfo.setColumnName(propertyInfo.getColumnName());
                newPropertyInfo.setTableInfo(tableInfo);
                newPropertyInfo.setJoinTableInfo(joinTableInfo);
                newPropertyInfo.setJavaType(propertyInfo.getJavaType());
                newPropertyInfo.setJdbcType(propertyInfo.getJdbcType());
                newPropertyInfo.setResultType(propertyInfo.getResultType());
                newPropertyInfo.setIdType(propertyInfo.getIdType());
                newPropertyInfo.setCustomIdGenerator(propertyInfo.getCustomIdGenerator());
                return newPropertyInfo;
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public ResultMapping getResultMapping(Configuration configuration, String resultMapId) {
        if (ResultType.ID == resultType) {
            return new ResultMapping.Builder(configuration, name)
                    .column(joinTableInfo.getAlias() + "_" + columnName)
                    .javaType(javaType)
                    .jdbcType(jdbcType)
                    .flags(Collections.singletonList(ResultFlag.ID))
                    .build();
        }
        if (ResultType.RESULT == resultType) {
            return new ResultMapping.Builder(configuration, name)
                    .column(joinTableInfo.getAlias() + "_" + columnName)
                    .javaType(javaType)
                    .jdbcType(jdbcType)
                    .build();
        }
        if (ResultType.ASSOCIATION == resultType) {
            String nestedResultMapId = resultMapId + "[association" + "=" + name + "]";
            new ResultMapBuilder(configuration).buildNestedResultMap(this, nestedResultMapId);
            return new ResultMapping.Builder(configuration, name)
                    .javaType(javaType)
                    .nestedResultMapId(nestedResultMapId)
                    .build();
        }
        if (ResultType.COLLECTION == resultType) {
            String nestedResultMapId = resultMapId + "[collection" + "=" + name + "]";
            new ResultMapBuilder(configuration).buildNestedResultMap(this, nestedResultMapId);
            return new ResultMapping.Builder(configuration, name)
                    .javaType(ofType)
                    .nestedResultMapId(nestedResultMapId)
                    .build();
        }
        throw new MybatisExtException("Unknown resultType: " + resultType);
    }
}
