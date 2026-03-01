package io.github.mybatisext.metadata;

import java.util.Objects;

import io.github.mybatisext.reflect.GenericType;

public class PropertyKey {

    private final GenericType ownerType;
    private final String name;

    public PropertyKey(GenericType ownerType, String name) {
        this.ownerType = ownerType;
        this.name = name;
    }

    public GenericType getOwnerType() {
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
