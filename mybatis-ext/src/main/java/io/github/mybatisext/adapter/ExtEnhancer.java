package io.github.mybatisext.adapter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import io.github.mybatisext.statement.MappedStatementHelper;
import io.github.mybatisext.util.CommonUtils;
import io.github.mybatisext.util.TypeArgumentResolver;
import javassist.util.proxy.MethodHandler;

public class ExtEnhancer implements MethodHandler {

    private static final MethodSignature GET_MAPPED_STATEMENT = new MethodSignature("getMappedStatement", new Class[]{String.class});
    private static final MethodSignature HAS_STATEMENT = new MethodSignature("hasStatement", new Class[]{String.class});
    private static final MethodSignature VALIDATE_ALL_MAPPER_METHOD = new MethodSignature("validateAllMapperMethod", new Class[]{});

    private final Configuration configuration;
    private final MappedStatementHelper mappedStatementHelper;

    private Map<String, Class<?>> mapperCache = Collections.emptyMap();

    public ExtEnhancer(Configuration configuration, ExtContext extContext) {
        this.configuration = configuration;
        this.mappedStatementHelper = new MappedStatementHelper(configuration, extContext);
    }

    @Override
    public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
        if (GET_MAPPED_STATEMENT.match(thisMethod)) {
            return getMappedStatement((String) args[0]);
        }
        if (HAS_STATEMENT.match(thisMethod)) {
            return hasStatement((String) args[0]);
        }
        if (VALIDATE_ALL_MAPPER_METHOD.match(thisMethod)) {
            validateAllMapperMethod();
            return null;
        }
        return thisMethod.invoke(configuration, args);
    }

    public MappedStatement getMappedStatement(String id) {
        if (configuration.hasStatement(id)) {
            return configuration.getMappedStatement(id);
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
        ms = buildMappedStatement(id, mapperClass, methodName, true);
        if (ms != null) {
            synchronized (configuration) {
                if (!configuration.hasStatement(ms.getId())) {
                    configuration.addMappedStatement(ms);
                }
            }
        }
        return ms;
    }

    public boolean hasStatement(String statementName) {
        if (configuration.hasStatement(statementName)) {
            return true;
        }
        MappedStatement ms = getMappedStatement(statementName);
        if (ms != null) {
            return Objects.equals(ms.getId(), statementName);
        }
        return false;
    }

    public void validateAllMapperMethod() {
        for (Class<?> mapperClass : configuration.getMapperRegistry().getMappers()) {
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
                    ms = buildMappedStatement(id, mapperClass, method.getName(), false);
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
        if (configuration.hasStatement(statementId)) {
            return configuration.getMappedStatement(statementId);
        }
        for (Class<?> superInterface : mapperClass.getInterfaces()) {
            MappedStatement ms = resolveMappedStatement(superInterface, methodName);
            if (ms != null) {
                return ms;
            }
        }
        return null;
    }

    private MappedStatement buildMappedStatement(String id, Class<?> mapperClass, String methodName, boolean changeConfiguration) {
        if (isNotEnhancedMapper(mapperClass)) {
            return null;
        }
        GenericType tableType = getEntityClass(mapperClass);
        GenericType mapperType = GenericTypeFactory.build(mapperClass);
        GenericType returnType = null;
        List<GenericMethod> methods = new ArrayList<>();
        for (GenericMethod method : mapperType.getMethods()) {
            if (method.isBridge() || method.isDefault() || !method.getName().equals(methodName)) {
                continue;
            }
            GenericType mReturnType = CommonUtils.unwrapType(method.getGenericReturnType());
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
        return mappedStatementHelper.build(id, tableType, methods, returnType, changeConfiguration);
    }

    private boolean isNotEnhancedMapper(Class<?> mapperClass) {
        return !mapperClass.isInterface() || mapperClass.getTypeParameters().length > 0 || (!mapperClass.isAnnotationPresent(MapTable.class) && !ExtMapper.class.isAssignableFrom(mapperClass));
    }

    private GenericType getEntityClass(Class<?> mapperClass) {
        MapTable annotation = mapperClass.getAnnotation(MapTable.class);
        if (annotation != null) {
            return GenericTypeFactory.build(annotation.value());
        }
        return TypeArgumentResolver.resolveGenericType(mapperClass, ExtMapper.class, 0);
    }

    private Class<?> getMapperClass(String namespace) {
        Collection<Class<?>> mappers = configuration.getMapperRegistry().getMappers();
        if (!mapperCache.containsKey(namespace)) {
            mapperCache = mappers.stream().collect(Collectors.toMap(Class::getName, v -> v));
        }
        return mapperCache.get(namespace);
    }
}
