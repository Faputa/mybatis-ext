package io.github.mybatisext.metadata;

import java.util.Objects;

public class JoinColumnFeature {

    private String leftColumn;
    private String rightColumn;
    private JoinTableInfo leftTable;
    private TableInfo rightTable;

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

    public JoinTableInfo getLeftTable() {
        return leftTable;
    }

    public void setLeftTable(JoinTableInfo leftTable) {
        this.leftTable = leftTable;
    }

    public TableInfo getRightTable() {
        return rightTable;
    }

    public void setRightTable(TableInfo rightTable) {
        this.rightTable = rightTable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JoinColumnFeature that = (JoinColumnFeature) o;
        return Objects.equals(leftColumn, that.leftColumn)
                && Objects.equals(rightColumn, that.rightColumn)
                && Objects.equals(leftTable, that.leftTable)
                && Objects.equals(rightTable, that.rightTable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftColumn, rightColumn, leftTable, rightTable);
    }
}
