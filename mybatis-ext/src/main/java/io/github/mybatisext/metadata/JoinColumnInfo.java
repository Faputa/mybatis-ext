package io.github.mybatisext.metadata;

public class JoinColumnInfo {

    private String leftColumn;
    private String rightColumn;

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
    public String toString() {
        return "left." + leftColumn + "=right." + rightColumn;
    }
}
