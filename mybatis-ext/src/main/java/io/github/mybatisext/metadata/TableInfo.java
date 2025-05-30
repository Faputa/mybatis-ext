package io.github.mybatisext.metadata;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.util.StringUtils;

public class TableInfo {

    /** 表名 */
    private String name;
    /** 表注释 */
    private String comment;
    /** 模式 */
    private String schema;
    /** 实体类 */
    private GenericType tableClass;
    /** 连接图 */
    private JoinTableInfo joinTableInfo;
    /** 别名到关联表的映射 */
    private final Map<String, JoinTableInfo> aliasToJoinTableInfo = new HashMap<>();
    /** 名字到属性的映射 */
    private final Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();
    /** 名字到列的映射 */
    private final Map<String, ColumnInfo> nameToColumnInfo = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public GenericType getTableClass() {
        return tableClass;
    }

    public void setTableClass(GenericType tableClass) {
        this.tableClass = tableClass;
    }

    public JoinTableInfo getJoinTableInfo() {
        return joinTableInfo;
    }

    public void setJoinTableInfo(JoinTableInfo joinTableInfo) {
        this.joinTableInfo = joinTableInfo;
    }

    public Map<String, JoinTableInfo> getAliasToJoinTableInfo() {
        return aliasToJoinTableInfo;
    }

    public Map<String, PropertyInfo> getNameToPropertyInfo() {
        return nameToPropertyInfo;
    }

    public Map<String, ColumnInfo> getNameToColumnInfo() {
        return nameToColumnInfo;
    }

    @Override
    public String toString() {
        return StringUtils.isNotBlank(schema) ? schema + "." + name : name;
    }
}
