package io.github.mybatisext.metadata;

import java.util.Objects;

public class ColumnInfo {

    /** 字段名 */
    private String name;
    /** 注释 */
    private String comment;
    /** 是否可空 */
    private boolean nullable;
    /** DDL语句 */
    private String columnDefinition;
    /** 长度 */
    private int length;
    /** 精度 */
    private int precision;
    /** 标度 */
    private int scale;
    /** 只读 */
    private boolean readonly;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public String getColumnDefinition() {
        return columnDefinition;
    }

    public void setColumnDefinition(String columnDefinition) {
        this.columnDefinition = columnDefinition;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ColumnInfo that = (ColumnInfo) o;
        return nullable == that.nullable && length == that.length && precision == that.precision && scale == that.scale && readonly == that.readonly && Objects.equals(name, that.name) && Objects.equals(comment, that.comment) && Objects.equals(columnDefinition, that.columnDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, comment, nullable, columnDefinition, length, precision, scale, readonly);
    }
}
