package com.mybatisext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;

import com.mybatisext.annotation.MapTable;
import com.mybatisext.mapper.ExtMapper;
import com.mybatisext.statement.MappedStatementBuilder;
import com.mybatisext.util.AnnotationResolver;
import com.mybatisext.util.TypeArgumentResolver;

public class ExtEnhancer {

    private final Configuration configuration;
    private final MappedStatementBuilder statementBuilder;
    private final Set<String> builtStatementId = ConcurrentHashMap.newKeySet();;

    private Map<String, Class<?>> mapperCache = Collections.emptyMap();

    public ExtEnhancer(Configuration configuration) {
        this.configuration = configuration;
        this.statementBuilder = new MappedStatementBuilder(configuration);
    }

    public MappedStatement getMappedStatement(String id) {
        MappedStatement mappedStatement = this.configuration.getMappedStatement(id);
        if (mappedStatement != null) {
            return mappedStatement;
        }
        mappedStatement = resolveMappedStatement(id);
        if (mappedStatement != null) {
            if (Objects.equals(mappedStatement.getId(), id)) {
                return mappedStatement;
            }
        }
        return null;
    }

    public boolean hasStatement(String statementName) {
        boolean hasStatement = this.configuration.hasStatement(statementName);
        if (hasStatement) {
            return true;
        }
        MappedStatement ms = resolveMappedStatement(statementName);
        if (ms != null) {
            return Objects.equals(ms.getId(), statementName);
        }
        return false;
    }

    public void validateAllMapperMethod() {
        for (Class<?> mapperClass : configuration.getMapperRegistry().getMappers()) {
            if (!isEnhancedMapper(mapperClass)) {
                continue;
            }
            for (Method method : mapperClass.getMethods()) {
                if (isBridgeOrDefault(method)) {
                    continue;
                }
                String statementId = mapperClass.getName() + "." + method.getName();
                MappedStatement ms = resolveMappedStatement(statementId);
                if (ms == null) {
                    throw new BindingException("Invalid bound statement (not found): " + statementId);
                }
            }
        }
    }

    private MappedStatement resolveMappedStatement(String id) {
        int lastIndexOf = id.lastIndexOf(".");
        if (lastIndexOf < 0) {
            return null;
        }
        String namespace = id.substring(0, lastIndexOf);
        String methodName = id.substring(lastIndexOf + 1);
        Class<?> mapperClass = getMapperClass(namespace);
        if (mapperClass == null) {
            return null;
        }
        MappedStatement ms = resolveMappedStatement(mapperClass, methodName);
        if (ms != null) {
            return ms;
        }
        if (!builtStatementId.contains(id)) {
            builtStatementId.add(id);
            return buildMappedStatement(id, methodName, mapperClass);
        }
        return null;
    }

    private MappedStatement resolveMappedStatement(Class<?> mapperClass, String methodName) {
        if (mapperClass == null || mapperClass == Object.class) {
            return null;
        }
        String statementId = mapperClass.getName() + "." + methodName;
        if (configuration.hasStatement(statementId)) {
            return configuration.getMappedStatement(statementId);
        }
        for (Class<?> superInterface : mapperClass.getInterfaces()) {
            MappedStatement ms = resolveMappedStatement(superInterface, methodName);
            if (ms != null) {
                return ms;
            }
        }
        return resolveMappedStatement(mapperClass.getSuperclass(), methodName);
    }

    private MappedStatement buildMappedStatement(String id, String methodName, Class<?> mapperClass) {
        if (!isEnhancedMapper(mapperClass)) {
            return null;
        }
        Class<?> tableType = getEntityClass(mapperClass);
        assert tableType != null;
        Class<?> returnType = null;
        List<Method> methods = new ArrayList<>();
        for (Method method : mapperClass.getMethods()) {
            if (isBridgeOrDefault(method)) {
                continue;
            }
            if (method.getName().equals(methodName)) {
                if (returnType == null || returnType.isAssignableFrom(method.getReturnType())) {
                    returnType = method.getReturnType();
                } else if (!method.getReturnType().isAssignableFrom(returnType)
                        && Void.class != method.getReturnType()) {
                    throw new IllegalArgumentException("returnType inconsistency: " + id);
                }
                methods.add(method);
            }
        }
        MappedStatement ms = statementBuilder.build(id, methodName, methods, returnType, tableType);
        if (ms != null) {
            configuration.addMappedStatement(ms);
        }
        return ms;
    }

    private boolean isEnhancedMapper(Class<?> mapperClass) {
        return mapperClass.isInterface() && !isGenericClass(mapperClass)
                && (ExtMapper.class.isAssignableFrom(mapperClass)
                        || AnnotationResolver.hasAnnotation(mapperClass, MapTable.class));
    }

    private boolean isGenericClass(Class<?> type) {
        return type.getTypeParameters().length > 0;
    }

    private boolean isBridgeOrDefault(Method method) {
        return method.isBridge() || method.isDefault();
    }

    private Class<?> getEntityClass(Class<?> mapperClass) {
        MapTable annotation = AnnotationResolver.findAnnotation(mapperClass, MapTable.class);
        if (annotation != null) {
            return annotation.value();
        }
        return TypeArgumentResolver.findClass(mapperClass, ExtMapper.class, 0);
    }

    private Class<?> getMapperClass(String namespace) {
        Collection<Class<?>> mappers = configuration.getMapperRegistry().getMappers();
        if (!mapperCache.containsKey(namespace)) {
            mapperCache = mappers.stream().collect(Collectors.toMap(Class::getName, v -> v));
        }
        return mapperCache.get(namespace);
    }

}
