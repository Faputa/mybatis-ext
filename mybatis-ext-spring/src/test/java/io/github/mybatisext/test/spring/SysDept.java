package io.github.mybatisext.test.spring;

import java.util.Objects;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.EmbedParent;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;

/**
 * 部门表 sys_dept
 * 
 * @author ruoyi
 */
@Table
@EmbedParent
public class SysDept extends BaseEntity {

    /** 部门ID */
    @Id
    @Column
    private Long deptId;

    /** 父部门ID */
    @Column
    private Long parentId;

    /** 祖级列表 */
    @Column
    private String ancestors;

    /** 部门名称 */
    @Column
    private String deptName;

    /** 显示顺序 */
    @Column
    private Integer orderNum;

    /** 负责人 */
    @Column
    private String leader;

    /** 联系电话 */
    @Column
    private String phone;

    /** 邮箱 */
    @Column
    private String email;

    /** 部门状态:0正常,1停用 */
    @Column
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    @Column
    private String delFlag;

    /** 父部门名称 */
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "parentId", rightColumn = "deptId"), table = SysDept.class, column = "deptName")
    private String parentName;

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

    public String getAncestors() {
        return ancestors;
    }

    public void setAncestors(String ancestors) {
        this.ancestors = ancestors;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SysDept sysDept = (SysDept) o;
        return Objects.equals(deptId, sysDept.deptId) && Objects.equals(parentId, sysDept.parentId) && Objects.equals(ancestors, sysDept.ancestors) && Objects.equals(deptName, sysDept.deptName) && Objects.equals(orderNum, sysDept.orderNum) && Objects.equals(leader, sysDept.leader) && Objects.equals(phone, sysDept.phone) && Objects.equals(email, sysDept.email) && Objects.equals(status, sysDept.status) && Objects.equals(delFlag, sysDept.delFlag) && Objects.equals(parentName, sysDept.parentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deptId, parentId, ancestors, deptName, orderNum, leader, phone, email, status, delFlag, parentName);
    }

    @Override
    public String toString() {
        return "SysDept{" +
                "deptId=" + deptId +
                ", parentId=" + parentId +
                ", ancestors='" + ancestors + '\'' +
                ", deptName='" + deptName + '\'' +
                ", orderNum=" + orderNum +
                ", leader='" + leader + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                ", delFlag='" + delFlag + '\'' +
                ", parentName='" + parentName + '\'' +
                '}';
    }
}
