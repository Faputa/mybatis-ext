package io.github.mybatisext.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.github.mybatisext.diagnostic.DiagnosticLocation;

public final class JavaTypes {

    private JavaTypes() {
    }

    private static JavaType findSupertype(JavaType type, Class<?> target, Set<JavaType> visited) {
        if (type == null || !visited.add(type)) {
            return null;
        }
        if (!type.isResolved()) {
            throw new UnresolvedTypeException(type.getName());
        }
        if (type.is(target)) {
            return type;
        }
        for (JavaType parent : type.getGenericInterfaces()) {
            JavaType found = findSupertype(parent, target, visited);
            if (found != null) {
                return found;
            }
        }
        return findSupertype(type.getGenericSuperclass(), target, visited);
    }

    public static JavaType typeArgument(JavaType type, Class<?> owner, int index) {
        JavaType parent = findSupertype(type, owner, new HashSet<>());
        if (parent == null || parent.getTypeParameters().length <= index) {
            throw new UnresolvedTypeException(type.getName() + "<" + owner.getTypeName() + "> argument " + index);
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
        if (type.isAssignableTo(Collection.class)) {
            return typeArgument(type, Collection.class, 0);
        }
        if (type.isAssignableTo(Optional.class)) {
            return typeArgument(type, Optional.class, 0);
        }
        return type;
    }
}
