package io.github.mybatisext.metadata;

import io.github.mybatisext.annotation.Cascade;
import io.github.mybatisext.annotation.ColumnRef;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinParent;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.JoinRelations;
import io.github.mybatisext.annotation.Fetch;
import io.github.mybatisext.annotation.TableRef;

import java.sql.Timestamp;
import java.util.List;

@TableRef(value = TablePermission.class, alias = "tp")
@JoinParent(alias = "dt", joinColumn = @JoinColumn(leftColumn = "tableId", rightColumn = "id"))
public class TablePermissionVO extends DataTable {

    @ColumnRef
    private String tableId;
    @ColumnRef
    private String roleId;
    @ColumnRef
    private String roleIdAndTableId;
    @ColumnRef
    private String permissionType;
    @ColumnRef
    private Timestamp createdAt;
    @ColumnRef
    private Timestamp updatedAt;

    @ColumnRef
    private String description;
    @ColumnRef
    private String dataSourceName3;

    // ========回显字段========

    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "tableId", rightColumn = "id"))
    private DataTable dataTable;
    @Cascade
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "tableId", rightColumn = "id"))
    private DataTable dataTable2;

    @JoinRelation(joinColumn = @JoinColumn(leftTableAlias = "dt", leftColumn = "dataSourceId", rightColumn = "id"), table = DataSource.class, column = "id")
    private String dataSourceId;

    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "tableId", rightColumn = "id"), table = DataTable.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "dataSourceId", rightColumn = "id"), table = DataSource.class, column = "name")
    private String dataSourceName;

    @JoinRelation(joinColumn = @JoinColumn(leftTableAlias = "dt", leftColumn = "dataSourceId", rightColumn = "id"), table = DataSource.class, column = "name")
    private String dataSourceName2;

    @JoinRelations({
            @JoinRelation(joinColumn = @JoinColumn(leftColumn = "tableId", rightColumn = "id"), table = DataTable.class, column = "tableName")
    })
    private String dataTableName;

    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "tableId", rightColumn = "id"), table = DataTable.class)
    private String schemaName;

    @JoinRelation(joinColumn = {
            @JoinColumn(leftColumn = "tableId", rightColumn = "tableId"),
            @JoinColumn(leftColumn = "roleId", rightColumn = "roleId")
    })
    private List<RowPermission> rowPermissions;

    @Fetch
    @JoinRelation(joinColumn = {
            @JoinColumn(leftColumn = "tableId", rightColumn = "tableId"),
            @JoinColumn(leftColumn = "roleId", rightColumn = "roleId")
    })
    private List<RowPermission> rowPermissions2;

    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "tableId", rightColumn = "id"), table = DataTable.class, tableAlias = "a")
    @JoinRelation(joinColumn = {
            @JoinColumn(leftTableAlias = "a", leftColumn = "id", rightColumn = "tableId"),
            @JoinColumn(leftTableAlias = "tp", leftColumn = "roleId", rightColumn = "roleId")
    })
    private List<ColumnPermission> columnPermissions;

    public String getTableId() {
        return tableId;
    }

    @ColumnRef
    public String getTableId2() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getDataSourceName2() {
        return dataSourceName2;
    }

    public void setDataSourceName2(String dataSourceName2) {
        this.dataSourceName2 = dataSourceName2;
    }

    public String getDataTableName() {
        return dataTableName;
    }

    public void setDataTableName(String dataTableName) {
        this.dataTableName = dataTableName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public List<RowPermission> getRowPermissions() {
        return rowPermissions;
    }

    public void setRowPermissions(List<RowPermission> rowPermissions) {
        this.rowPermissions = rowPermissions;
    }

    public List<RowPermission> getRowPermissions2() {
        return rowPermissions2;
    }

    public void setRowPermissions2(List<RowPermission> rowPermissions2) {
        this.rowPermissions2 = rowPermissions2;
    }

    public List<ColumnPermission> getColumnPermissions() {
        return columnPermissions;
    }

    public void setColumnPermissions(List<ColumnPermission> columnPermissions) {
        this.columnPermissions = columnPermissions;
    }
}
