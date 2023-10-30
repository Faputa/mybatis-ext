package com.example;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;

public class Configuration extends org.apache.ibatis.session.Configuration {

    private Map<String, Class<?>> mapperCache = new HashMap<>();

    public Configuration(Environment environment) {
        super(environment);
    }

    @Override
    public void addMappers(String packageName, Class<?> superType) {
        super.addMappers(packageName, superType);
        refreshMapperCache();
    }

    @Override
    public void addMappers(String packageName) {
        super.addMappers(packageName);
        refreshMapperCache();
    }

    @Override
    public <T> void addMapper(Class<T> type) {
        super.addMapper(type);
        refreshMapperCache();
    }

    @Override
    public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {
        boolean hasStatement = super.hasStatement(statementName, validateIncompleteStatements);
        if (hasStatement) {
            return true;
        }
        // TODO
        return false;
    }

    @Override
    public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
        MappedStatement mappedStatement = super.getMappedStatement(id, validateIncompleteStatements);
        if (mappedStatement != null) {
            return mappedStatement;
        }
        int lastIndexOf = id.lastIndexOf(".");
        String mapperName = id.substring(0, id.lastIndexOf("."));
        String methodName = id.substring(lastIndexOf);

        Class<?> mapper = mapperCache.get(mapperName);
        if (mapper == null) {
            return null;
        }

        for (Class<?> superInterface : mapper.getInterfaces()) {
            if (mapper.isAssignableFrom(superInterface)) {
            }
        }

        for (Method method : mapper.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
            }
        }
        // TODO
        return null;
    }

    private void refreshMapperCache() {
        Collection<Class<?>> mappers = mapperRegistry.getMappers();
        if (mapperCache.size() != mappers.size()) {
            mapperCache = mappers.stream().collect(Collectors.toMap(v -> v.getName(), v -> v));
        }
    }

}
