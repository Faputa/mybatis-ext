package io.github.mybatisext.table;

import java.io.Serializable;
import java.sql.Timestamp;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;

@Table
public class MetadataConnection implements Serializable {

    @Id
    @Column
    private String id;
    @Column
    private String name;
    @Column
    private String driverId;
    @Column
    private String category;
    @Column
    private String className;
    @Column
    private String applicationId;
    @Column
    private String server;
    @Column
    private String port;
    @Column
    private String dbName;
    @Column
    private String userName;
    @Column
    private String password;
    @Column
    private String url;
    @Column
    private String metaDataConnectionId;
    @Column
    private Timestamp createTime;
    @Column
    private Timestamp updateTime;
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMetaDataConnectionId() {
        return metaDataConnectionId;
    }

    public void setMetaDataConnectionId(String metaDataConnectionId) {
        this.metaDataConnectionId = metaDataConnectionId;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }
}
