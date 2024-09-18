package io.github.mybatisext.jpa;

import java.util.Objects;

public class Limit implements Modifier {

    private Integer offset;
    private Integer rowCount;
    private String offsetVariable;
    private String rowCountVariable;

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public String getOffsetVariable() {
        return offsetVariable;
    }

    public void setOffsetVariable(String offsetVariable) {
        this.offsetVariable = offsetVariable;
    }

    public String getRowCountVariable() {
        return rowCountVariable;
    }

    public void setRowCountVariable(String rowCountVariable) {
        this.rowCountVariable = rowCountVariable;
    }

    @Override
    public void accept(Semantic semantic) {
        semantic.setLimit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Limit limit = (Limit) o;
        return Objects.equals(offset, limit.offset) && Objects.equals(rowCount, limit.rowCount) && Objects.equals(offsetVariable, limit.offsetVariable) && Objects.equals(rowCountVariable, limit.rowCountVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, rowCount, offsetVariable, rowCountVariable);
    }
}
