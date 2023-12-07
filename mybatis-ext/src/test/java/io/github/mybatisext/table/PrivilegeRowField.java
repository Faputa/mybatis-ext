package io.github.mybatisext.table;

import java.util.Objects;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;

@Table
public class PrivilegeRowField {

    @Id
    @Column
    private String tableId;
    @Id
    @Column
    private String userId;
    @Id
    @Column
    private String field;
    @Id
    @Column
    private String relation;
    @Id
    @Column
    private Integer targetType;
    @Id
    @Column
    private String target;

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

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    // 用于去重
    @Override
    public int hashCode() {
        return Objects.hash(tableId, userId, field, relation, targetType, target);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PrivilegeRowField other = (PrivilegeRowField) obj;
        return Objects.equals(tableId, other.tableId) &&
                Objects.equals(userId, other.userId) &&
                Objects.equals(field, other.field) &&
                Objects.equals(relation, other.relation) &&
                Objects.equals(targetType, other.targetType) &&
                Objects.equals(target, other.target);
    }
}
