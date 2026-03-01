package io.github.mybatisext.metadata;

import io.github.mybatisext.util.StringUtils;

import java.util.Objects;

public class TableName {

    private String name;
    private String schema;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TableName tableName = (TableName) o;
        return Objects.equals(name, tableName.name) && Objects.equals(schema, tableName.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, schema);
    }

    @Override
    public String toString() {
        if (StringUtils.isNotBlank(schema)) {
            return schema + "." + name;
        }
        return name;
    }
}
