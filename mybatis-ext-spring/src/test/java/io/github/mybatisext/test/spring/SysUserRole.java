package io.github.mybatisext.test.spring;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;

/**
 * 用户和角色关联 sys_user_role
 * 
 * @author ruoyi
 */
@Table
public class SysUserRole {

    /** 用户ID */
    @Id
    @Column
    private Long userId;

    /** 角色ID */
    @Id
    @Column
    private Long roleId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
