package com.example;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

public class MjConfiguration extends Configuration {

    private static final Log logger = LogFactory.getLog(MjConfiguration.class);

    private Map<String, Class<?>> mapperCache = Collections.emptyMap();

    public MjConfiguration(Environment environment) {
        super(environment);
    }

    public void validateAllMapperMethod(boolean panicIfStatementNotFound) {
        for (Class<?> mapperInterface : mapperRegistry.getMappers()) {
            for (Method method : mapperInterface.getDeclaredMethods()) {
                String statementId = mapperInterface.getName() + "." + method.getName();
                if (resolveMappedStatement(statementId) == null) {
                    String INVALID_STATEMENT_MESSAGE = "Invalid bound statement (not found): " + statementId;
                    if (panicIfStatementNotFound) {
                        throw new BindingException(INVALID_STATEMENT_MESSAGE);
                    }
                    logger.warn(INVALID_STATEMENT_MESSAGE);
                }
            }
        }
    }

    @Override
    public boolean hasStatement(String statementName) {
        boolean hasStatement = super.hasStatement(statementName);
        if (hasStatement) {
            return true;
        }
        MappedStatement ms = resolveMappedStatement(statementName);
        if (ms != null) {
            if (Objects.equals(ms.getId(), statementName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MappedStatement getMappedStatement(String id) {
        MappedStatement mappedStatement = super.getMappedStatement(id);
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

    private MappedStatement resolveMappedStatement(String id) {
        int lastIndexOf = id.lastIndexOf(".");
        if (lastIndexOf < 0) {
            return null;
        }
        String namespace = id.substring(0, lastIndexOf);
        String methodName = id.substring(lastIndexOf + 1);
        Class<?> mapperInterface = getMapperInterface(namespace);
        if (mapperInterface == null) {
            return null;
        }
        MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, mapperInterface);
        if (ms != null) {
            return ms;
        }
        return buildMappedStatement(id, methodName, mapperInterface);
    }

    private MappedStatement buildMappedStatement(String id, String methodName, Class<?> mapperInterface) {
        MjMapper mjMapper = mapperInterface.getAnnotation(MjMapper.class);
        if (mjMapper == null) {
            return null;
        }
        Class<?> tableType = mjMapper.value();
        Class<?> returnType = null;
        List<Parameter[]> parametersList = new ArrayList<>();
        for (Method method : mapperInterface.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                if (returnType == null) {
                    returnType = method.getReturnType();
                }
                if (!returnType.equals(method.getReturnType()) && !Void.class.equals(method.getReturnType())) {
                    throw new IllegalArgumentException("returnType inconsistency: " + id);
                }
                parametersList.add(method.getParameters());
            }
        }
        MappedStatement ms = buildMappedStatement(id, methodName, parametersList, returnType, tableType);
        if (ms != null) {
            addMappedStatement(ms);
        }
        return ms;
    }

    private MappedStatement buildMappedStatement(String id, String methodName, List<Parameter[]> parametersList,
            Class<?> returnType, Class<?> tableType) {
        if (!"countDidaTask2".equals(methodName)) {
            return null;
        }
        // TODO
        XMLLanguageDriver xmlLanguageDriver = new XMLLanguageDriver();
        SqlSource sqlSource = xmlLanguageDriver.createSqlSource(this, "<script>select count(*) from org</script>",
                Object.class);
        List<ResultMap> resultMaps = new ArrayList<>();
        ResultMap inlineResultMap = new ResultMap.Builder(this, id + "-Inline", Integer.class, new ArrayList<>(), null)
                .build();
        resultMaps.add(inlineResultMap);
        Builder builder = new MappedStatement.Builder(this, id, sqlSource, SqlCommandType.SELECT);
        return builder.resultMaps(resultMaps).resultSetType(ResultSetType.DEFAULT).build();
    }

    private Class<?> getMapperInterface(String namespace) {
        Collection<Class<?>> mappers = mapperRegistry.getMappers();
        if (!mapperCache.containsKey(namespace)) {
            mapperCache = mappers.stream().collect(Collectors.toMap(v -> v.getName(), v -> v));
        }
        Class<?> mapperInterface = mapperCache.get(namespace);
        return mapperInterface;
    }

    private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
            Class<?> declaringClass) {
        String statementId = mapperInterface.getName() + "." + methodName;
        if (mappedStatements.containsKey(statementId)) {
            return mappedStatements.get(statementId);
        }
        if (mapperInterface.equals(declaringClass)) {
            return null;
        }
        for (Class<?> superInterface : mapperInterface.getInterfaces()) {
            if (declaringClass.isAssignableFrom(superInterface)) {
                MappedStatement ms = resolveMappedStatement(superInterface, methodName, declaringClass);
                if (ms != null) {
                    return ms;
                }
            }
        }
        return null;
    }

}
