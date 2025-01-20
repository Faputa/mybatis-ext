package io.github.mybatisext.test;

import java.util.List;
import java.util.Objects;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.EmbedParent;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.LoadStrategy;
import io.github.mybatisext.annotation.LoadType;
import io.github.mybatisext.annotation.Table;

/**
 * 角色表 sys_role
 *
 * @author ruoyi
 */
@Table
@EmbedParent
public class SysRole extends BaseEntity {

    /** 角色ID */
    @Id
    @Column
    private Long roleId;

    /** 角色名称 */
    @Column
    private String roleName;

    /** 角色权限 */
    @Column
    private String roleKey;

    /** 角色排序 */
    @Column
    private String roleSort;

    /** 数据范围（1：所有数据权限；2：自定义数据权限；3：本部门数据权限；4：本部门及以下数据权限；5：仅本人数据权限） */
    @Column
    private String dataScope;

    /** 角色状态（0正常 1停用） */
    @Column
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    @Column
    private String delFlag;

    /** 菜单组 */
    @LoadStrategy(LoadType.FETCH_EAGER)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "roleId", rightColumn = "roleId"), table = SysRoleMenu.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "menuId", rightColumn = "menuId"), table = SysMenu.class, column = "menuId")
    private List<Long> menuIds;

    /** 部门组（数据权限） */
    @LoadStrategy(LoadType.FETCH_EAGER)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "roleId", rightColumn = "roleId"), table = SysRoleDept.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "deptId", rightColumn = "deptId"), table = SysDept.class, column = "deptId")
    private List<Long> deptIds;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public boolean isAdmin() {
        return isAdmin(this.roleId);
    }

    public static boolean isAdmin(Long roleId) {
        return roleId != null && 1L == roleId;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
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

    public String getStatus() {
        return status;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public void setStatus(String status) {
        this.status = status;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SysRole sysRole = (SysRole) o;
        return Objects.equals(roleId, sysRole.roleId) && Objects.equals(roleName, sysRole.roleName) && Objects.equals(roleKey, sysRole.roleKey) && Objects.equals(roleSort, sysRole.roleSort) && Objects.equals(dataScope, sysRole.dataScope) && Objects.equals(status, sysRole.status) && Objects.equals(delFlag, sysRole.delFlag) && Objects.equals(menuIds, sysRole.menuIds) && Objects.equals(deptIds, sysRole.deptIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, roleName, roleKey, roleSort, dataScope, status, delFlag, menuIds, deptIds);
    }

    @Override
    public String toString() {
        return "SysRole{" +
                "roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", roleKey='" + roleKey + '\'' +
                ", roleSort='" + roleSort + '\'' +
                ", dataScope='" + dataScope + '\'' +
                ", status='" + status + '\'' +
                ", delFlag='" + delFlag + '\'' +
                ", menuIds=" + menuIds +
                ", deptIds=" + deptIds +
                '}';
    }
}
