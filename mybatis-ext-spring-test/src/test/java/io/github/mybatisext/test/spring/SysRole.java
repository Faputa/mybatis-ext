package io.github.mybatisext.test.spring;

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
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "role_id", rightColumn = "role_id"), table = SysRoleMenu.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "menu_id", rightColumn = "menu_id"), table = SysMenu.class, column = "menu_id")
    private Long[] menuIds;

    /** 部门组（数据权限） */
    @LoadStrategy(LoadType.FETCH_EAGER)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "role_id", rightColumn = "role_id"), table = SysRoleDept.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "dept_id", rightColumn = "dept_id"), table = SysDept.class, column = "dept_id")
    private Long[] deptIds;

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

    public Long[] getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(Long[] menuIds) {
        this.menuIds = menuIds;
    }

    public Long[] getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(Long[] deptIds) {
        this.deptIds = deptIds;
    }
}
