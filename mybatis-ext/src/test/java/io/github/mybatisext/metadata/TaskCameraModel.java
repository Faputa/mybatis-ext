package io.github.mybatisext.metadata;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.RelatedColumn;
import io.github.mybatisext.annotation.RelatedOn;
import io.github.mybatisext.annotation.RelatedTable;
import io.github.mybatisext.annotation.RelatedTables;
import io.github.mybatisext.annotation.Table;

@Table
@RelatedTable(tableClass = Task.class, relatedOn = @RelatedOn(column = "task_id", relatedColumn = "task_id"))
@RelatedTable(tableClass = Camera.class, relatedOn = @RelatedOn(column = "camera_id", relatedColumn = "camera_id"))
@RelatedTable(tableClass = Model.class, relatedOn = @RelatedOn(column = "model_id", relatedColumn = "model_id"))
public class TaskCameraModel {

    public static void main(String[] args) {
        RelatedTable[] relatedTables = TaskCameraModel.class.getAnnotationsByType(RelatedTable.class);
        RelatedTables relatedTables2 = TaskCameraModel.class.getAnnotation(RelatedTables.class);
        System.out.println(relatedTables);
        System.out.println(relatedTables2);
    }

    @Id
    @Column
    private Long taskId;
    @Id
    @Column
    private Long cameraId;
    @Id
    @Column
    private Long modelId;

    @RelatedColumn(tableClass = Task.class)
    private String taskName;
    @RelatedColumn(tableClass = Camera.class)
    private String cameraName;
    @RelatedColumn(tableClass = Model.class)
    private String modelName;

    @RelatedColumn
    private Org org;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getCameraId() {
        return cameraId;
    }

    public void setCameraId(Long cameraId) {
        this.cameraId = cameraId;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Org getOrg() {
        return org;
    }

    public void setOrg(Org org) {
        this.org = org;
    }
}
