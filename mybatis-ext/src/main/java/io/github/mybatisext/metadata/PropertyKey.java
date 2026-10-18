package io.github.mybatisext.metadata;

import java.util.Objects;

import io.github.mybatisext.model.JavaType;

public class PropertyKey {

    private final JavaType ownerType;
    private final String name;

    public PropertyKey(JavaType ownerType, String name) {
        this.ownerType = ownerType;
        this.name = name;
    }

    public JavaType getOwnerType() {
        return ownerType;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PropertyKey that = (PropertyKey) o;
        return Objects.equals(ownerType, that.ownerType) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerType, name);
    }
}
