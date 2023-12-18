package io.github.mybatisext.table;

import java.util.List;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.JoinRelations;
import io.github.mybatisext.annotation.Table;

@Table
public class PrivilegeTable {

    @Id
    @Column
    private String tableId;
    @Id
    @Column
    private String userId;
    @Column
    private Boolean rowPrivilegeEnabled;
    @Column
    private Boolean colPrivilegeEnabled;
    @Column
    private Boolean tablePrivilegeEnabled;
    @Column
    private String rowPrivilegeRelation;
    @Column
    private String tableForbidden;
    @Column
    private Boolean enabled;
    @Column
    private String createUser;
    @Column
    private String updateUser;
    @Column
    private java.sql.Timestamp createTime;
    @Column
    private java.sql.Timestamp updateTime;

    // ========回显字段========

    // https://www.yuque.com/dontang/codewiki/sfydxf
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "table_id", rightColumn = "id"), table = MetadataTable.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "connection_id", rightColumn = "id"), table = MetadataConnection.class, column = "name")
    private String dbName;

    @JoinRelations({
            @JoinRelation(joinColumn = @JoinColumn(leftColumn = "table_id", rightColumn = "id"), table = MetadataTable.class, column = "name")
    })
    private String tableName;

    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "table_id", rightColumn = "id"), table = MetadataTable.class)
    private String connectionId;

    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "table_id", rightColumn = "id"), table = MetadataTable.class)
    private String schemaName;

    @JoinRelation(joinColumn = {
            @JoinColumn(leftColumn = "table_id", rightColumn = "table_id"),
            @JoinColumn(leftColumn = "user_id", rightColumn = "user_id")
    })
    private List<PrivilegeRowField> rowPrivilegeFields;

    @JoinRelation(joinColumn = {
            @JoinColumn(leftColumn = "table_id", rightColumn = "table_id"),
            @JoinColumn(leftColumn = "user_id", rightColumn = "user_id")
    })
    private List<PrivilegeColField> colPrivilegeFields;

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean isRowPrivilegeEnabled() {
        return rowPrivilegeEnabled;
    }

    public void setRowPrivilegeEnabled(Boolean rowPrivilegeEnabled) {
        this.rowPrivilegeEnabled = rowPrivilegeEnabled;
    }

    public Boolean isColPrivilegeEnabled() {
        return colPrivilegeEnabled;
    }

    public void setColPrivilegeEnabled(Boolean colPrivilegeEnabled) {
        this.colPrivilegeEnabled = colPrivilegeEnabled;
    }

    public Boolean isTablePrivilegeEnabled() {
        return tablePrivilegeEnabled;
    }

    public void setTablePrivilegeEnabled(Boolean tablePrivilegeEnabled) {
        this.tablePrivilegeEnabled = tablePrivilegeEnabled;
    }

    public String getRowPrivilegeRelation() {
        return rowPrivilegeRelation;
    }

    public void setRowPrivilegeRelation(String rowPrivilegeRelation) {
        this.rowPrivilegeRelation = rowPrivilegeRelation;
    }

    public String getTableForbidden() {
        return tableForbidden;
    }

    public void setTableForbidden(String tableForbidden) {
        this.tableForbidden = tableForbidden;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public java.sql.Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.sql.Timestamp createTime) {
        this.createTime = createTime;
    }

    public java.sql.Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(java.sql.Timestamp updateTime) {
        this.updateTime = updateTime;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<PrivilegeRowField> getRowPrivilegeFields() {
        return rowPrivilegeFields;
    }

    public void setRowPrivilegeFields(List<PrivilegeRowField> rowPrivilegeFields) {
        this.rowPrivilegeFields = rowPrivilegeFields;
    }

    public List<PrivilegeColField> getColPrivilegeFields() {
        return colPrivilegeFields;
    }

    public void setColPrivilegeFields(List<PrivilegeColField> colPrivilegeFields) {
        this.colPrivilegeFields = colPrivilegeFields;
    }

}
