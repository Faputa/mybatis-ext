package io.github.mybatisext.metadata;

public class JoinColumnInfo {

    private JoinTableInfo leftJoinTableInfo;
    private String leftFullName;
    private String leftColumnName;
    private String rightFullName;
    private String rightColumnName;

    public JoinTableInfo getLeftJoinTableInfo() {
        return leftJoinTableInfo;
    }

    public void setLeftJoinTableInfo(JoinTableInfo leftJoinTableInfo) {
        this.leftJoinTableInfo = leftJoinTableInfo;
    }

    public String getLeftFullName() {
        return leftFullName;
    }

    public void setLeftFullName(String leftFullName) {
        this.leftFullName = leftFullName;
    }

    public String getLeftColumnName() {
        return leftColumnName;
    }

    public void setLeftColumnName(String leftColumnName) {
        this.leftColumnName = leftColumnName;
    }

    public String getRightFullName() {
        return rightFullName;
    }

    public void setRightFullName(String rightFullName) {
        this.rightFullName = rightFullName;
    }

    public String getRightColumnName() {
        return rightColumnName;
    }

    public void setRightColumnName(String rightColumnName) {
        this.rightColumnName = rightColumnName;
    }
}
