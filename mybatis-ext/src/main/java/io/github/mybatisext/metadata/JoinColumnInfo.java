package io.github.mybatisext.metadata;

public class JoinColumnInfo {

    private PropertyInfo leftColumn;
    private PropertyInfo rightColumn;

    public PropertyInfo getLeftColumn() {
        return leftColumn;
    }

    public void setLeftColumn(PropertyInfo leftColumn) {
        this.leftColumn = leftColumn;
    }

    public PropertyInfo getRightColumn() {
        return rightColumn;
    }

    public void setRightColumn(PropertyInfo rightColumn) {
        this.rightColumn = rightColumn;
    }

    @Override
    public String toString() {
        return "left." + leftColumn.getFullName() + "=right." + rightColumn.getFullName();
    }
}
