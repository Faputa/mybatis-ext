package io.github.mybatisext.test.flex;

import java.sql.Timestamp;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;

@Table
@com.mybatisflex.annotation.Table("camera")
public class Camera {

    @Id
    @Column
    private Long cameraId;
    @Column
    private Long orgId;
    @Column
    private String cameraName;
    @Column
    private String cameraCode;
    @Column
    private String cameraMemo;
    @Column
    private String ip;
    @Column
    private Integer port;
    @Column
    private String username;
    @Column
    private String password;
    @Column
    private String streamType;
    @Column
    private String site;
    @Column
    private String vendor;
    @Column
    private String model;
    @Column
    private Integer interval;
    @Column
    private Boolean enabled;
    @Column
    private Boolean emergencyEnabled;
    @Column
    private Boolean emergencyAllowed;
    @Column
    private Integer status;
    @Column
    private Timestamp insertTime;
    @Column
    private Timestamp updateTime;
    @Column
    private Boolean deleted;

    public Long getCameraId() {
        return cameraId;
    }

    public void setCameraId(Long cameraId) {
        this.cameraId = cameraId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getCameraCode() {
        return cameraCode;
    }

    public void setCameraCode(String cameraCode) {
        this.cameraCode = cameraCode;
    }

    public String getCameraMemo() {
        return cameraMemo;
    }

    public void setCameraMemo(String cameraMemo) {
        this.cameraMemo = cameraMemo;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStreamType() {
        return streamType;
    }

    public void setStreamType(String streamType) {
        this.streamType = streamType;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getEmergencyEnabled() {
        return emergencyEnabled;
    }

    public void setEmergencyEnabled(Boolean emergencyEnabled) {
        this.emergencyEnabled = emergencyEnabled;
    }

    public Boolean getEmergencyAllowed() {
        return emergencyAllowed;
    }

    public void setEmergencyAllowed(Boolean emergencyAllowed) {
        this.emergencyAllowed = emergencyAllowed;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
