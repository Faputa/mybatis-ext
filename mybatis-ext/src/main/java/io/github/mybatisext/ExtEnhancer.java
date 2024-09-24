package io.github.mybatisext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.MapTable;
import io.github.mybatisext.mapper.BaseMapper;
import io.github.mybatisext.mapper.ExtMapper;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.statement.MappedStatementBuilder;
import io.github.mybatisext.util.TypeArgumentResolver;

public class ExtEnhancer {

    private final Configuration originConfiguration;
    private final MappedStatementBuilder statementBuilder;
    private final Object lock = new Object();

    private Map<String, Class<?>> mapperCache = Collections.emptyMap();

    public ExtEnhancer(Configuration originConfiguration, ExtContext extContext) {
        this.originConfiguration = originConfiguration;
        this.statementBuilder = new MappedStatementBuilder(originConfiguration, extContext);
    }

    public MappedStatement getMappedStatement(String id) {
        if (originConfiguration.hasStatement(id)) {
            return originConfiguration.getMappedStatement(id);
        }
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
        ms = buildMappedStatement(id, mapperClass, methodName);
        if (ms != null) {
            synchronized (lock) {
                if (!originConfiguration.hasStatement(id)) {
                    originConfiguration.addMappedStatement(ms);
                }
            }
        }
        return ms;
    }

    public boolean hasStatement(String statementName) {
        if (originConfiguration.hasStatement(statementName)) {
            return true;
        }
        MappedStatement ms = getMappedStatement(statementName);
        if (ms != null) {
            return Objects.equals(ms.getId(), statementName);
        }
        return false;
    }

    public void validateAllMapperMethod() {
        for (Class<?> mapperClass : originConfiguration.getMapperRegistry().getMappers()) {
            if (isNotEnhancedMapper(mapperClass)) {
                continue;
            }
            GenericType genericType = GenericTypeFactory.build(mapperClass);
            for (GenericMethod method : genericType.getMethods()) {
                if (method.isBridge() || method.isDefault() || method.getDeclaringClass() == BaseMapper.class) {
                    continue;
                }
                String id = mapperClass.getName() + "." + method.getName();
                MappedStatement ms = resolveMappedStatement(mapperClass, method.getName());
                if (ms == null) {
                    ms = buildMappedStatement(id, mapperClass, method.getName());
                }
                if (ms == null) {
                    throw new BindingException("Invalid bound statement (not found): " + id);
                }
            }
        }
    }

    private MappedStatement resolveMappedStatement(Class<?> mapperClass, String methodName) {
        if (mapperClass == null || mapperClass == Object.class) {
            return null;
        }
        String statementId = mapperClass.getName() + "." + methodName;
        if (originConfiguration.hasStatement(statementId)) {
            return originConfiguration.getMappedStatement(statementId);
        }
        for (Class<?> superInterface : mapperClass.getInterfaces()) {
            MappedStatement ms = resolveMappedStatement(superInterface, methodName);
            if (ms != null) {
                return ms;
            }
        }
        return null;
    }

    private MappedStatement buildMappedStatement(String id, Class<?> mapperClass, String methodName) {
        if (isNotEnhancedMapper(mapperClass)) {
            return null;
        }
        GenericType tableType = getEntityClass(mapperClass);
        GenericType genericType = GenericTypeFactory.build(mapperClass);
        GenericType returnType = null;
        List<GenericMethod> methods = new ArrayList<>();
        for (GenericMethod method : genericType.getMethods()) {
            if (method.isBridge() || method.isDefault() || !method.getName().equals(methodName)) {
                continue;
            }
            GenericType mReturnType = method.getGenericReturnType();
            if (Collection.class.isAssignableFrom(mReturnType.getType())) {
                mReturnType = TypeArgumentResolver.resolveGenericTypeArgument(method.getGenericReturnType(), Collection.class, 0);
            } else if (mReturnType.getType() == Optional.class) {
                mReturnType = TypeArgumentResolver.resolveGenericTypeArgument(method.getGenericReturnType(), Optional.class, 0);
            }
            if (returnType == null || returnType.isAssignableFrom(mReturnType)) {
                returnType = mReturnType;
            } else if (!mReturnType.isAssignableFrom(returnType) && mReturnType.getType() != Void.class) {
                throw new IllegalArgumentException("returnType inconsistency: " + mapperClass.getName() + "." + methodName);
            }
            methods.add(method);
        }
        if (methods.isEmpty()) {
            return null;
        }
        return statementBuilder.build(id, tableType, methods, returnType);
    }

    private boolean isNotEnhancedMapper(Class<?> mapperClass) {
        return !mapperClass.isInterface() || mapperClass.getTypeParameters().length > 0 || (!mapperClass.isAnnotationPresent(MapTable.class) && !ExtMapper.class.isAssignableFrom(mapperClass));
    }

    private GenericType getEntityClass(Class<?> mapperClass) {
        MapTable annotation = mapperClass.getAnnotation(MapTable.class);
        if (annotation != null) {
            return GenericTypeFactory.build(annotation.value());
        }
        return TypeArgumentResolver.resolveGenericTypeArgument(mapperClass, ExtMapper.class, 0);
    }

    private Class<?> getMapperClass(String namespace) {
        Collection<Class<?>> mappers = originConfiguration.getMapperRegistry().getMappers();
        if (!mapperCache.containsKey(namespace)) {
            mapperCache = mappers.stream().collect(Collectors.toMap(Class::getName, v -> v));
        }
        return mapperCache.get(namespace);
    }
}
