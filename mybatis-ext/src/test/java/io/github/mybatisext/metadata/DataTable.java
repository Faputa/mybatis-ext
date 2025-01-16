package io.github.mybatisext.metadata;

import java.sql.Timestamp;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinParent;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;

@Table
@JoinParent(joinColumn = @JoinColumn(leftColumn = "dataSourceId", rightColumn = "id"))
public class DataTable extends DataSource {

    @Id
    @Column
    private String id;
    @Column
    private String dataSourceId;
    @Column
    private String tableName;
    @Column
    private String schemaName;
    @Column
    private String description;
    @Column
    private Timestamp createdAt;
    @Column
    private Timestamp updatedAt;

    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "dataSourceId", rightColumn = "id"), table = DataSource.class, column = "name")
    private String dataSourceName3;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
