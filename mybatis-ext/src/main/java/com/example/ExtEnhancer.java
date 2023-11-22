package com.example;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

public class ExtEnhancer {

    private final Configuration configuration;
    private final Set<String> builtStatementId = ConcurrentHashMap.newKeySet();;
    private Map<String, Class<?>> mapperCache = Collections.emptyMap();

    public ExtEnhancer(Configuration configuration) {
        this.configuration = configuration;
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

    private MappedStatement buildMappedStatement(String id, String methodName, Class<?> mapperClass) {
        if (!isEnhancedMapper(mapperClass)) {
            return null;
        }
        Class<?> tableType = null;
        Class<?> returnType = null;
        List<Parameter[]> parametersList = new ArrayList<>();
        for (Method method : mapperClass.getMethods()) {
            if (isBridgeOrDefault(method)) {
                continue;
            }
            if (method.getName().equals(methodName)) {
                Class<?> entityClass = getEntityClass(mapperClass, method);
                assert entityClass != null;
                if (tableType == null) {
                    tableType = entityClass;
                }
                if (tableType != entityClass) {
                    throw new IllegalArgumentException("tableType inconsistency: " + id);
                }
                if (returnType == null) {
                    returnType = method.getReturnType();
                }
                if (returnType != method.getReturnType() && Void.class != method.getReturnType()) {
                    throw new IllegalArgumentException("returnType inconsistency: " + id);
                }
                parametersList.add(method.getParameters());
            }
        }
        MappedStatement ms = buildMappedStatement(id, methodName, parametersList, returnType, tableType);
        if (ms != null) {
            configuration.addMappedStatement(ms);
        }
        return ms;
    }

    private boolean isEnhancedMapper(Class<?> mapperClass) {
        return mapperClass.isInterface() && !isGenericClass(mapperClass)
                && (mapperClass.getAnnotation(Mapping.class) != null
                        || ExtMapper.class.isAssignableFrom(mapperClass));
    }

    private boolean isGenericClass(Class<?> type) {
        return type.getTypeParameters().length > 0;
    }

    private boolean isBridgeOrDefault(Method method) {
        return method.isBridge() || method.isDefault();
    }

    private Class<?> getEntityClass(Class<?> mapperClass, Method method) {
        return getEntityClass(mapperClass, method, null);
    }

    private Class<?> getEntityClass(Class<?> mapperClass, Method method, Type[] actualTypeArguments) {
        // 检查方法是否在继承路径上
        if (method != null && !mapperClass.isAssignableFrom(method.getDeclaringClass())
                && !method.getDeclaringClass().isAssignableFrom(mapperClass)) {
            return null;
        }
        // 检查注解
        Mapping mapping = mapperClass.getAnnotation(Mapping.class);
        if (mapping != null) {
            return mapping.value();
        }
        // 检查基接口
        Type[] genericInterfaces = mapperClass.getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
                // 泛型基接口
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type rawType = parameterizedType.getRawType();
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                adjustTypeArguments(mapperClass, actualTypeArguments, typeArguments);
                if (rawType == ExtMapper.class) {
                    // 找到了
                    if (typeArguments[0] instanceof Class) {
                        return (Class<?>) typeArguments[0];
                    }
                } else if (rawType instanceof Class) {
                    // 其他泛型基接口
                    Class<?> entityClass = getEntityClass((Class<?>) rawType, method, typeArguments);
                    if (entityClass != null) {
                        return entityClass;
                    }
                }
            } else if (type instanceof Class) {
                // 其他基接口
                Class<?> entityClass = getEntityClass((Class<?>) type, method);
                if (entityClass != null) {
                    return entityClass;
                }
            }
        }
        // 检查基类
        Class<?> superclass = mapperClass.getSuperclass();
        if (superclass == null || superclass == Object.class) {
            return null;
        }
        Type[] typeArguments = superclass.getTypeParameters();
        adjustTypeArguments(mapperClass, actualTypeArguments, typeArguments);
        return getEntityClass(superclass, method, typeArguments);
    }

    private void adjustTypeArguments(Class<?> subclass, Type[] subclassTypeArguments, Type[] typeArguments) {
        for (int i = 0; i < typeArguments.length; i++) {
            if (typeArguments[i] instanceof TypeVariable) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) typeArguments[i];
                TypeVariable<?>[] typeParameters = subclass.getTypeParameters();
                for (int j = 0; j < typeParameters.length; j++) {
                    if (Objects.equals(typeVariable.getName(), typeParameters[j].getName())) {
                        typeArguments[i] = subclassTypeArguments[j];
                        break;
                    }
                }
            }
        }
    }

    private MappedStatement buildMappedStatement(String id, String methodName, List<Parameter[]> parametersList,
            Class<?> returnType, Class<?> tableType) {
        if (!"countDidaTask2".equals(methodName)) {
            return null;
        }
        // TODO
        // 考虑自己实现SqlSource
        XMLLanguageDriver xmlLanguageDriver = new XMLLanguageDriver();
        SqlSource sqlSource = xmlLanguageDriver.createSqlSource(configuration,
                "<script>select count(*) from org</script>",
                Object.class);
        List<ResultMap> resultMaps = new ArrayList<>();
        ResultMap inlineResultMap = new ResultMap.Builder(configuration, id + "-Inline", Integer.class,
                new ArrayList<>(), null)
                .build();
        resultMaps.add(inlineResultMap);
        Builder builder = new MappedStatement.Builder(configuration, id, sqlSource, SqlCommandType.SELECT);
        return builder.resultMaps(resultMaps).resultSetType(ResultSetType.DEFAULT).build();
    }

    private Class<?> getMapperClass(String namespace) {
        Collection<Class<?>> mappers = configuration.getMapperRegistry().getMappers();
        if (!mapperCache.containsKey(namespace)) {
            mapperCache = mappers.stream().collect(Collectors.toMap(Class::getName, v -> v));
        }
        return mapperCache.get(namespace);
    }

    private MappedStatement resolveMappedStatement(Class<?> mapperClass, String methodName) {
        if (mapperClass == null) {
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

}
