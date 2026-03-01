package io.github.mybatisext.metadata;

import java.util.Objects;
import java.util.Set;

import io.github.mybatisext.reflect.GenericType;

public class JoinNode {

    private Set<JoinPath> leftJoinPaths;
    private GenericType tableType;
    private String alias;

    public Set<JoinPath> getLeftJoinPaths() {
        return leftJoinPaths;
    }

    public void setLeftJoinPaths(Set<JoinPath> leftJoinPaths) {
        this.leftJoinPaths = leftJoinPaths;
    }

    public GenericType getTableType() {
        return tableType;
    }

    public void setTableType(GenericType tableType) {
        this.tableType = tableType;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JoinNode joinNode = (JoinNode) o;
        return Objects.equals(leftJoinPaths, joinNode.leftJoinPaths) && Objects.equals(tableType, joinNode.tableType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftJoinPaths, tableType);
    }

    @Override
    public String toString() {
        return alias;
    }
}
