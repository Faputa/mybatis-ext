package io.github.mybatisext.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableInfo {

    /** 表名 */
    private String name;
    /** 表注释 */
    private String comment;
    /** 模式 */
    private String schema;
    /** 列 */
    private List<ColumnInfo> columnInfos = new ArrayList<>();
    /** 连接图 */
    private JoinTableInfo joinTableInfo;
    /** 别名到关联表的映射 */
    private Map<String, JoinTableInfo> aliasToJoinTableInfo = new HashMap<>();
    /** 名字到属性的映射 */
    private Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();

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

    public List<ColumnInfo> getColumnInfos() {
        return columnInfos;
    }

    public void setColumnInfos(List<ColumnInfo> columnInfos) {
        this.columnInfos = columnInfos;
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

    public void setAliasToJoinTableInfo(Map<String, JoinTableInfo> aliasToJoinTableInfo) {
        this.aliasToJoinTableInfo = aliasToJoinTableInfo;
    }

    public Map<String, PropertyInfo> getNameToPropertyInfo() {
        return nameToPropertyInfo;
    }

    public void setNameToPropertyInfo(Map<String, PropertyInfo> nameToPropertyInfo) {
        this.nameToPropertyInfo = nameToPropertyInfo;
    }
}
