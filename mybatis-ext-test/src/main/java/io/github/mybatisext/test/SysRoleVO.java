package io.github.mybatisext.test;

import java.util.List;

import io.github.mybatisext.annotation.ColumnRef;
import io.github.mybatisext.annotation.TableRef;

@TableRef(SysRole.class)
public class SysRoleVO {

    @ColumnRef
    private Long roleId;
    @ColumnRef
    private String roleName;
    @ColumnRef
    private String roleKey;
    @ColumnRef
    private String roleSort;
    @ColumnRef
    private String dataScope;
    @ColumnRef
    private String status;
    @ColumnRef
    private String delFlag;
    @ColumnRef
    private List<Long> menuIds;
    @ColumnRef
    private List<Long> deptIds;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public String getRoleSort() {
        return roleSort;
    }

    public void setRoleSort(String roleSort) {
        this.roleSort = roleSort;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public List<Long> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<Long> menuIds) {
        this.menuIds = menuIds;
    }

    public List<Long> getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(List<Long> deptIds) {
        this.deptIds = deptIds;
    }
}
