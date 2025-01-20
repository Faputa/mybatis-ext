package io.github.mybatisext.test;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;

/**
 * 角色和菜单关联 sys_role_menu
 * 
 * @author ruoyi
 */
@Table
public class SysRoleMenu {

    /** 角色ID */
    @Id
    @Column
    private Long roleId;

    /** 菜单ID */
    @Id
    @Column
    private Long menuId;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }
}
