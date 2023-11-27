package io.github.mybatisext.test.spring;

import java.sql.Timestamp;

public class Org {

    private Long orgId;
    private Long pid;
    private String orgName;
    private String orgCode;
    private String fastCode;
    private Timestamp insertTime;
    private Timestamp updateTime;
    private Boolean deleted;

    public Long getId() {
        return orgId;
    }

    public void setId(Long id) {
        this.orgId = id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getPid() {
        return pid;
    }

    public void setPid(Long pid) {
        this.pid = pid;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public String getFastCode() {
        if (fastCode == null) {
            return "";
        }
        return fastCode;
    }

    public void setFastCode(String fastCode) {
        this.fastCode = fastCode;
    }

    public Timestamp getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(Timestamp insertTime) {
        this.insertTime = insertTime;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
