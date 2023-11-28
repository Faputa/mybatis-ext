package io.github.mybatisext.metadata;

public class ColumnInfo {

    /** 列名 */
    private String name;
    /** 列注释 */
    private String comment;
    /** 属性 */
    private String property;
    /** 是否主键 */
    private boolean primary = false;
    /** 是否可空 */
    private boolean nullable = true;

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

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }
}
