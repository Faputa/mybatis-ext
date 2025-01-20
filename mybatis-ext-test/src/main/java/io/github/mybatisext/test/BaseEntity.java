package io.github.mybatisext.test;

import java.util.Date;

import io.github.mybatisext.annotation.Column;

/**
 * Entity基类
 * 
 * @author ruoyi
 */
public class BaseEntity {

    /** 创建者 */
    @Column
    private String createBy;

    /** 创建时间 */
    @Column
    private Date createTime;

    /** 更新者 */
    @Column
    private String updateBy;

    /** 更新时间 */
    @Column
    private Date updateTime;

    /** 备注 */
    @Column
    private String remark;

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
