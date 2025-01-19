package io.github.mybatisext.test.spring;

import java.util.Date;
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
 * 用户对象 sys_user
 *
 * @author ruoyi
 */
@Table
@EmbedParent
public class SysUser extends BaseEntity {

    /** 用户ID */
    @Id
    @Column
    private Long userId;

    /** 部门ID */
    @Column
    private Long deptId;

    /** 部门父ID */
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "deptId", rightColumn = "deptId"), table = SysDept.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "parentId", rightColumn = "deptId"), table = SysDept.class, column = "deptId")
    private Long parentId;

    /** 登录名称 */
    @Column
    private String loginName;

    /** 用户名称 */
    @Column
    private String userName;

    /** 用户类型 */
    @Column
    private String userType;

    /** 用户邮箱 */
    @Column
    private String email;

    /** 手机号码 */
    @Column
    private String phonenumber;

    /** 用户性别 */
    @Column
    private String sex;

    /** 用户头像 */
    @Column
    private String avatar;

    /** 密码 */
    @Column
    private String password;

    /** 盐加密 */
    @Column
    private String salt;

    /** 帐号状态（0正常 1停用） */
    @Column
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    @Column
    private String delFlag;

    /** 最后登录IP */
    @Column
    private String loginIp;

    /** 最后登录时间 */
    @Column
    private Date loginDate;

    /** 密码最后更新时间 */
    @Column
    private Date pwdUpdateDate;

    /** 部门对象 */
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "deptId", rightColumn = "deptId"))
    private SysDept dept;

    @LoadStrategy(LoadType.FETCH_EAGER)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "userId", rightColumn = "userId"), table = SysUserRole.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "roleId", rightColumn = "roleId"))
    private List<SysRole> roles;

    /** 角色组 */
    @LoadStrategy(LoadType.FETCH_EAGER)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "userId", rightColumn = "userId"), table = SysUserRole.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "roleId", rightColumn = "roleId"), table = SysRole.class, column = "roleId")
    private List<Long> roleIds;

    /** 岗位组 */
    @LoadStrategy(LoadType.FETCH_EAGER)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "userId", rightColumn = "userId"), table = SysUserPost.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "postId", rightColumn = "postId"), table = SysPost.class, column = "postId")
    private List<Long> postIds;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isAdmin() {
        return isAdmin(this.userId);
    }

    public static boolean isAdmin(Long userId) {
        return userId != null && 1L == userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
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

    public String getLoginIp() {
        return loginIp;
    }

    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }

    public Date getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(Date loginDate) {
        this.loginDate = loginDate;
    }

    public Date getPwdUpdateDate() {
        return pwdUpdateDate;
    }

    public void setPwdUpdateDate(Date pwdUpdateDate) {
        this.pwdUpdateDate = pwdUpdateDate;
    }

    public SysDept getDept() {
        if (dept == null) {
            dept = new SysDept();
        }
        return dept;
    }

    public void setDept(SysDept dept) {
        this.dept = dept;
    }

    public List<SysRole> getRoles() {
        return roles;
    }

    public void setRoles(List<SysRole> roles) {
        this.roles = roles;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public List<Long> getPostIds() {
        return postIds;
    }

    public void setPostIds(List<Long> postIds) {
        this.postIds = postIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SysUser sysUser = (SysUser) o;
        return Objects.equals(userId, sysUser.userId) && Objects.equals(deptId, sysUser.deptId) && Objects.equals(parentId, sysUser.parentId) && Objects.equals(loginName, sysUser.loginName) && Objects.equals(userName, sysUser.userName) && Objects.equals(userType, sysUser.userType) && Objects.equals(email, sysUser.email) && Objects.equals(phonenumber, sysUser.phonenumber) && Objects.equals(sex, sysUser.sex) && Objects.equals(avatar, sysUser.avatar) && Objects.equals(password, sysUser.password) && Objects.equals(salt, sysUser.salt) && Objects.equals(status, sysUser.status) && Objects.equals(delFlag, sysUser.delFlag) && Objects.equals(loginIp, sysUser.loginIp) && Objects.equals(loginDate, sysUser.loginDate) && Objects.equals(pwdUpdateDate, sysUser.pwdUpdateDate) && Objects.equals(dept, sysUser.dept) && Objects.equals(roles, sysUser.roles) && Objects.equals(roleIds, sysUser.roleIds) && Objects.equals(postIds, sysUser.postIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, deptId, parentId, loginName, userName, userType, email, phonenumber, sex, avatar, password, salt, status, delFlag, loginIp, loginDate, pwdUpdateDate, dept, roles, roleIds, postIds);
    }

    @Override
    public String toString() {
        return "SysUser{" +
                "userId=" + userId +
                ", deptId=" + deptId +
                ", parentId=" + parentId +
                ", loginName='" + loginName + '\'' +
                ", userName='" + userName + '\'' +
                ", userType='" + userType + '\'' +
                ", email='" + email + '\'' +
                ", phonenumber='" + phonenumber + '\'' +
                ", sex='" + sex + '\'' +
                ", avatar='" + avatar + '\'' +
                ", password='" + password + '\'' +
                ", salt='" + salt + '\'' +
                ", status='" + status + '\'' +
                ", delFlag='" + delFlag + '\'' +
                ", loginIp='" + loginIp + '\'' +
                ", loginDate=" + loginDate +
                ", pwdUpdateDate=" + pwdUpdateDate +
                ", dept=" + dept +
                ", roles=" + roles +
                ", roleIds=" + roleIds +
                ", postIds=" + postIds +
                '}';
    }
}
