package io.github.mybatisext.jpa;

import java.util.Collection;
import java.util.Map;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.model.JavaBeanProperty;
import io.github.mybatisext.model.JavaField;
import io.github.mybatisext.model.JavaMethod;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.JavaTypes;
import io.github.mybatisext.model.TypePolicy;
import io.github.mybatisext.reflect.ReflectionTypePolicy;

public class VariableFactory {

    public static void addChildren(Configuration configuration, Variable variable) {
        addChildren(new ReflectionTypePolicy(configuration), variable);
    }

    public static void addChildren(TypePolicy typePolicy, Variable variable) {
        Map<String, Variable> children = variable.getNameToVariable();
        if (!children.isEmpty() || !hasSubVariable(typePolicy, variable.getJavaType())) {
            return;
        }
        JavaBeanProperty[] properties = variable.getJavaType().getBeanProperties();
        for (JavaType c = variable.getJavaType(); c != null && !JavaTypes.is(c, Object.class.getName()); c = c.getGenericSuperclass()) {
            for (JavaField field : c.getDeclaredFields()) {
                children.putIfAbsent(field.getName(), new Variable(variable.getFullName(), field.getName(), field.getGenericType(), field));
            }
            for (JavaBeanProperty property : properties) {
                JavaMethod readMethod = property.getReadMethod();
                if (readMethod != null && readMethod.getDeclaringType().equals(c)) {
                    children.putIfAbsent(property.getName(), new Variable(variable.getFullName(), property.getName(), readMethod.getGenericReturnType(), readMethod));
                }
            }
        }
    }

    public static boolean hasSubVariable(Configuration configuration, Class<?> type) {
        return !configuration.getTypeHandlerRegistry().hasTypeHandler(type)
                && !Map.class.isAssignableFrom(type)
                && !Collection.class.isAssignableFrom(type) && !type.isArray();
    }

    public static boolean hasSubVariable(TypePolicy policy, JavaType type) {
        return !policy.isScalar(type) && !type.isArray()
                && !JavaTypes.isAssignableTo(type, Map.class.getName())
                && !JavaTypes.isAssignableTo(type, Collection.class.getName());
    }
}
