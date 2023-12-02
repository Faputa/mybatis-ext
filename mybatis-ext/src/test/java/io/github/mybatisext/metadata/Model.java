package io.github.mybatisext.metadata;

import java.sql.Timestamp;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinTable;
import io.github.mybatisext.annotation.Table;

@Table
@JoinTable(TaskCameraModel.class)
public class Model {

    @Id
    @Column
    private Long modelId;
    @Column
    private Long programId;
    @Column
    private String modelName;
    @Column
    private String modelMemo;
    @Column
    private String modelCode;
    @Column
    private String orthoCode;
    @Column
    private Integer triggerNumber;
    @Column
    private Boolean enabled;
    @Column
    private Timestamp insertTime;
    @Column
    private Timestamp updateTime;
    @Column
    private Boolean deleted;

    public Long getModelId() {
        return this.modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public Long getProgramId() {
        return this.programId;
    }

    public void setProgramId(Long programId) {
        this.programId = programId;
    }

    public String getModelName() {
        return this.modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelMemo() {
        return this.modelMemo;
    }

    public void setModelMemo(String modelMemo) {
        this.modelMemo = modelMemo;
    }

    public String getModelCode() {
        return this.modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getOrthoCode() {
        return this.orthoCode;
    }

    public void setOrthoCode(String orthoCode) {
        this.orthoCode = orthoCode;
    }

    public Integer getTriggerNumber() {
        return this.triggerNumber;
    }

    public void setTriggerNumber(Integer triggerNumber) {
        this.triggerNumber = triggerNumber;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
