package io.github.mybatisext.metadata;

import java.util.ArrayList;
import java.util.List;

public class TableInfo {

    /** 表名 */
    private String name;
    /** 表注释 */
    private String comment;
    /** 列 */
    private List<ColumnInfo> columnInfos = new ArrayList<>();
    /** 连接表 */
    private List<JoinTableInfo> joinTableInfos = new ArrayList<>();

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

    public List<ColumnInfo> getColumnInfos() {
        return columnInfos;
    }

    public void setColumnInfos(List<ColumnInfo> columnInfos) {
        this.columnInfos = columnInfos;
    }

    public List<JoinTableInfo> getJoinTableInfos() {
        return joinTableInfos;
    }

    public void setJoinTableInfos(List<JoinTableInfo> joinTableInfos) {
        this.joinTableInfos = joinTableInfos;
    }
}
