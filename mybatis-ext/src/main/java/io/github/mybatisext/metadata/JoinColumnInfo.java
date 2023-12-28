package io.github.mybatisext.metadata;

import io.github.mybatisext.annotation.JoinColumn;

// TODO 考虑优化关联关系图结构，去除JoinColumnInfo
public class JoinColumnInfo {

    private JoinColumn joinColumn;
    private JoinTableInfo leftTable;
    private JoinTableInfo rightTable;

    public JoinColumn getJoinColumn() {
        return joinColumn;
    }

    public void setJoinColumn(JoinColumn joinColumn) {
        this.joinColumn = joinColumn;
    }

    public JoinTableInfo getLeftTable() {
        return leftTable;
    }

    public void setLeftTable(JoinTableInfo leftTable) {
        this.leftTable = leftTable;
    }

    public JoinTableInfo getRightTable() {
        return rightTable;
    }

    public void setRightTable(JoinTableInfo rightTable) {
        this.rightTable = rightTable;
    }
}
