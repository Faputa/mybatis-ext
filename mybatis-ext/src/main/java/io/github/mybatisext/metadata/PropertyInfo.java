package io.github.mybatisext.metadata;

import java.util.Collections;
import java.util.HashMap;

import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.idgenerator.IdGenerator;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.resultmap.ResultMapBuilder;
import io.github.mybatisext.resultmap.ResultType;

public class PropertyInfo extends HashMap<String, PropertyInfo> {

    private String name;
    // 如果是简单类型属性
    private String columnName;
    private TableInfo tableInfo;
    private JoinTableInfo joinTableInfo;
    private GenericType javaType;
    private JdbcType jdbcType;

    // resultMap项的类型
    private ResultType resultType;

    // resultType=ID
    private IdType idType;
    private IdGenerator<?> customIdGenerator;

    // resultType=COLLECTION
    private GenericType ofType;

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

    public GenericType getJavaType() {
        return javaType;
    }

    public void setJavaType(GenericType javaType) {
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

    public GenericType getOfType() {
        return ofType;
    }

    public void setOfType(GenericType ofType) {
        this.ofType = ofType;
    }

    public boolean isOwnColumn() {
        return joinTableInfo.getTableInfo() == tableInfo;
    }

    @Override
    public String toString() {
        return joinTableInfo.getAlias() + "." + columnName;
    }

    public ResultMapping getResultMapping(Configuration configuration, String resultMapId) {
        if (ResultType.ID == resultType) {
            return new ResultMapping.Builder(configuration, name)
                    .column(joinTableInfo.getAlias() + "_" + columnName)
                    .javaType(javaType.getType())
                    .jdbcType(jdbcType)
                    .flags(Collections.singletonList(ResultFlag.ID))
                    .build();
        }
        if (ResultType.RESULT == resultType) {
            return new ResultMapping.Builder(configuration, name)
                    .column(joinTableInfo.getAlias() + "_" + columnName)
                    .javaType(javaType.getType())
                    .jdbcType(jdbcType)
                    .build();
        }
        if (ResultType.ASSOCIATION == resultType) {
            String nestedResultMapId = resultMapId + "[association" + "=" + name + "]";
            ResultMapBuilder.addNestedResultMap(configuration, nestedResultMapId, this);
            return new ResultMapping.Builder(configuration, name)
                    .javaType(javaType.getType())
                    .nestedResultMapId(nestedResultMapId)
                    .build();
        }
        if (ResultType.COLLECTION == resultType) {
            String nestedResultMapId = resultMapId + "[collection" + "=" + name + "]";
            ResultMapBuilder.addNestedResultMap(configuration, nestedResultMapId, this);
            return new ResultMapping.Builder(configuration, name)
                    .javaType(ofType.getType())
                    .nestedResultMapId(nestedResultMapId)
                    .build();
        }
        throw new MybatisExtException("Unknown resultType: " + resultType);
    }
}
