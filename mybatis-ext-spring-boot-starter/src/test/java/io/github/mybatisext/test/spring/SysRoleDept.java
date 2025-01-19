package io.github.mybatisext.test.spring;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;

/**
 * 角色和部门关联 sys_role_dept
 * 
 * @author ruoyi
 */
@Table
public class SysRoleDept {

    /** 角色ID */
    @Id
    @Column
    private Long roleId;

    /** 部门ID */
    @Id
    @Column
    private Long deptId;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }
}
