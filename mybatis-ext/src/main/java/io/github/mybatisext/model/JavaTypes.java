package io.github.mybatisext.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.github.mybatisext.diagnostic.DiagnosticLocation;

public final class JavaTypes {

    private JavaTypes() {
    }

    public static boolean is(JavaType type, String name) {
        return type != null && type.isResolved() && name.equals(type.getName());
    }

    public static boolean isAssignableTo(JavaType type, String name) {
        return findSupertype(type, name, new HashSet<>()) != null;
    }

    private static JavaType findSupertype(JavaType type, String name, Set<JavaType> visited) {
        if (type == null || !visited.add(type)) {
            return null;
        }
        if (!type.isResolved()) {
            throw new UnresolvedTypeException(type.getName());
        }
        if (name.equals(type.getName())) {
            return type;
        }
        for (JavaType parent : type.getGenericInterfaces()) {
            JavaType found = findSupertype(parent, name, visited);
            if (found != null) {
                return found;
            }
        }
        return findSupertype(type.getGenericSuperclass(), name, visited);
    }

    public static JavaType typeArgument(JavaType type, String owner, int index) {
        JavaType parent = findSupertype(type, owner, new HashSet<>());
        if (parent == null || parent.getTypeParameters().length <= index) {
            throw new UnresolvedTypeException(type.getName() + "<" + owner + "> argument " + index);
        }
        return parent.getTypeParameters()[index];
    }

    public static JavaType unwrap(JavaType type, DiagnosticLocation location) {
        try {
            return unwrap(type);
        } catch (UnresolvedTypeException e) {
            throw e.at(location);
        }
    }

    public static JavaType unwrap(JavaType type) {
        if (type == null) {
            return null;
        }
        if (type.isArray()) {
            return type.getComponentType();
        }
        if (isAssignableTo(type, Collection.class.getName())) {
            return typeArgument(type, Collection.class.getName(), 0);
        }
        if (isAssignableTo(type, Optional.class.getName())) {
            return typeArgument(type, Optional.class.getName(), 0);
        }
        return type;
    }
}
