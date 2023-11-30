package io.github.mybatisext.metadata;

import java.sql.Timestamp;

import io.github.mybatisext.annotation.AlreadyRelatedTable;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;

@Table
@AlreadyRelatedTable(TaskCameraModel.class)
public class Task {

    @Id
    @Column
    private Long taskId;
    @Column
    private Long userId;
    @Column
    private String taskName;
    @Column
    private String taskMemo;
    @Column
    private Boolean pushEnabled;
    @Column
    private String timeRangeJson;
    @Column
    private Boolean enabled;
    @Column
    private Timestamp insertTime;
    @Column
    private Timestamp updateTime;
    @Column
    private Boolean deleted;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskMemo() {
        return taskMemo;
    }

    public void setTaskMemo(String taskMemo) {
        this.taskMemo = taskMemo;
    }

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public String getTimeRangeJson() {
        return timeRangeJson;
    }

    public void setTimeRangeJson(String timeRangeJson) {
        this.timeRangeJson = timeRangeJson;
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
