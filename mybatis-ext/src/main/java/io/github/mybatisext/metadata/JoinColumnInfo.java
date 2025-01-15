package io.github.mybatisext.metadata;

public class JoinColumnInfo {

    private String leftColumn;
    private String rightColumn;
    private PropertyInfo leftPropertyInfo;
    private PropertyInfo rightPropertyInfo;

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

    public PropertyInfo getLeftPropertyInfo() {
        return leftPropertyInfo;
    }

    public void setLeftPropertyInfo(PropertyInfo leftPropertyInfo) {
        this.leftPropertyInfo = leftPropertyInfo;
    }

    public PropertyInfo getRightPropertyInfo() {
        return rightPropertyInfo;
    }

    public void setRightPropertyInfo(PropertyInfo rightPropertyInfo) {
        this.rightPropertyInfo = rightPropertyInfo;
    }

    @Override
    public String toString() {
        return "left." + leftColumn + "=right." + rightColumn;
    }
}
