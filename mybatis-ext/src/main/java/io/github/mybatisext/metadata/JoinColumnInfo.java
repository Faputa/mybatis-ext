package io.github.mybatisext.metadata;

public class JoinColumnInfo {

    private JoinTableInfo leftJoinTableInfo;
    private PropertyDef leftColumn;
    private PropertyDef rightColumn;
    private String leftFullName;
    private String rightFullName;

    public JoinTableInfo getLeftJoinTableInfo() {
        return leftJoinTableInfo;
    }

    public void setLeftJoinTableInfo(JoinTableInfo leftJoinTableInfo) {
        this.leftJoinTableInfo = leftJoinTableInfo;
    }

    public PropertyDef getLeftColumn() {
        return leftColumn;
    }

    public void setLeftColumn(PropertyDef leftColumn) {
        this.leftColumn = leftColumn;
    }

    public PropertyDef getRightColumn() {
        return rightColumn;
    }

    public void setRightColumn(PropertyDef rightColumn) {
        this.rightColumn = rightColumn;
    }

    public String getLeftFullName() {
        return leftFullName;
    }

    public void setLeftFullName(String leftFullName) {
        this.leftFullName = leftFullName;
    }

    public String getRightFullName() {
        return rightFullName;
    }

    public void setRightFullName(String rightFullName) {
        this.rightFullName = rightFullName;
    }
}
