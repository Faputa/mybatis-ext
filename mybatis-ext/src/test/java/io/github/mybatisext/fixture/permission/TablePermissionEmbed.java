package io.github.mybatisext.fixture.permission;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;

import java.sql.Timestamp;

@Table(name = "table_permission")
public class TablePermissionEmbed {

    @Id
    @Column
    private String tableId;
    @Id
    @Column
    private String roleId;
    @Column
    private String roleIdAndTableId;
    @Column
    private String permissionType;
    @Column
    private Timestamp createdAt;
    @Column
    private Timestamp updatedAt;
    @Column
    private TablePermission tablePermission;

    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "tablePermission.tableId", rightColumn = "id"), table = DataTable.class)
    private String schemaName;

    public String getTableId() {
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

    public TablePermission getTablePermission() {
        return tablePermission;
    }

    public void setTablePermission(TablePermission tablePermission) {
        this.tablePermission = tablePermission;
    }
}
