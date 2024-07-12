package io.github.mybatisext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.MapTable;
import io.github.mybatisext.mapper.BaseMapper;
import io.github.mybatisext.mapper.ExtMapper;
import io.github.mybatisext.statement.MappedStatementBuilder;
import io.github.mybatisext.util.TypeArgumentResolver;

public class ExtEnhancer {

    private final Configuration originConfiguration;
    private final MappedStatementBuilder statementBuilder;
    private final Set<String> builtStatementId = ConcurrentHashMap.newKeySet();

    private static ThreadLocal<ExtEnhancer> instance = new ThreadLocal<>();

    public static ExtEnhancer getInstance() {
        return instance.get();
    }

    private Map<String, Class<?>> mapperCache = Collections.emptyMap();

    public ExtEnhancer(Configuration originConfiguration, ExtContext extContext) {
        this.originConfiguration = originConfiguration;
        this.statementBuilder = new MappedStatementBuilder(originConfiguration, extContext);
    }

    public MappedStatement getMappedStatement(String id) {
        instance.set(this);
        MappedStatement mappedStatement = this.originConfiguration.getMappedStatement(id);
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
        boolean hasStatement = this.originConfiguration.hasStatement(statementName);
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
        for (Class<?> mapperClass : originConfiguration.getMapperRegistry().getMappers()) {
            if (!isEnhancedMapper(mapperClass)) {
                continue;
            }
            for (Method method : mapperClass.getMethods()) {
                if (isBridgeOrDefault(method)) {
                    continue;
                }
                if (method.getDeclaringClass() == BaseMapper.class) {
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

    public Configuration getOriginConfiguration() {
        return originConfiguration;
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
        if (originConfiguration.hasStatement(statementId)) {
            return originConfiguration.getMappedStatement(statementId);
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
            if (isBridgeOrDefault(method) || !method.getName().equals(methodName)) {
                continue;
            }
            Class<?> mReturnType = method.getReturnType();
            if (mReturnType == Optional.class) {
                mReturnType = TypeArgumentResolver.resolveTypeArgument(method.getGenericReturnType(), Optional.class, 0);
            }
            assert mReturnType != null;
            if (returnType == null || returnType.isAssignableFrom(mReturnType)) {
                returnType = mReturnType;
            } else if (!mReturnType.isAssignableFrom(returnType) && Void.class != mReturnType) {
                throw new IllegalArgumentException("returnType inconsistency: " + id);
            }
            methods.add(method);
        }
        MappedStatement ms = statementBuilder.build(id, methodName, methods, returnType, tableType);
        if (ms != null) {
            originConfiguration.addMappedStatement(ms);
        }
        return ms;
    }

    private boolean isEnhancedMapper(Class<?> mapperClass) {
        return mapperClass.isInterface() && !isGenericClass(mapperClass)
                && (mapperClass.isAnnotationPresent(MapTable.class) || ExtMapper.class.isAssignableFrom(mapperClass));
    }

    private boolean isGenericClass(Class<?> type) {
        return type.getTypeParameters().length > 0;
    }

    private boolean isBridgeOrDefault(Method method) {
        return method.isBridge() || method.isDefault();
    }

    private Class<?> getEntityClass(Class<?> mapperClass) {
        MapTable annotation = mapperClass.getAnnotation(MapTable.class);
        if (annotation != null) {
            return annotation.value();
        }
        return TypeArgumentResolver.resolveTypeArgument(mapperClass, ExtMapper.class, 0);
    }

    private Class<?> getMapperClass(String namespace) {
        Collection<Class<?>> mappers = originConfiguration.getMapperRegistry().getMappers();
        if (!mapperCache.containsKey(namespace)) {
            mapperCache = mappers.stream().collect(Collectors.toMap(Class::getName, v -> v));
        }
        return mapperCache.get(namespace);
    }
}
