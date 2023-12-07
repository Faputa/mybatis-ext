package io.github.mybatisext.table;

import java.util.Objects;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;

@Table
public class PrivilegeColField {

    @Id
    @Column
    private String tableId;
    @Id
    @Column
    private String userId;
    @Id
    @Column
    private String field;
    @Column
    private Integer handleType;
    @Column
    private String handleStrategy;

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Integer getHandleType() {
        return handleType;
    }

    public void setHandleType(Integer handleType) {
        this.handleType = handleType;
    }

    public String getHandleStrategy() {
        return handleStrategy;
    }

    public void setHandleStrategy(String handleStrategy) {
        this.handleStrategy = handleStrategy;
    }

    // 用于去重
    @Override
    public int hashCode() {
        return Objects.hash(tableId, userId, field);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PrivilegeColField other = (PrivilegeColField) obj;
        return Objects.equals(tableId, other.tableId)
                && Objects.equals(userId, other.userId)
                && Objects.equals(field, other.field);
    }
}
