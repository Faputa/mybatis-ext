package io.github.mybatisext.metadata;

import java.util.Objects;

public class JoinPath {

    private JoinNode leftJoinNode;
    private String leftColumn;
    private String rightColumn;

    public JoinNode getLeftJoinNode() {
        return leftJoinNode;
    }

    public void setLeftJoinNode(JoinNode leftJoinNode) {
        this.leftJoinNode = leftJoinNode;
    }

    public String getLeftColumn() {
        return leftColumn;
    }

    public void setLeftColumn(String leftColumn) {
        this.leftColumn = leftColumn;
    }

    public String getRightColumn() {
        return rightColumn;
    }

    public void setRightColumn(String rightColumn) {
        this.rightColumn = rightColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JoinPath joinPath = (JoinPath) o;
        return Objects.equals(leftJoinNode, joinPath.leftJoinNode) && Objects.equals(leftColumn, joinPath.leftColumn) && Objects.equals(rightColumn, joinPath.rightColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftJoinNode, leftColumn, rightColumn);
    }
}
