package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.springframework.core.io.Resource;

public class ExtConfiguration extends Configuration {

    private static final Log logger = LogFactory.getLog(ExtConfiguration.class);

    private Map<String, Class<?>> mapperCache = Collections.emptyMap();
    private final Set<String> loadedStatementId = new HashSet<>();
    private Resource[] mapperLocations;

    public Resource[] getMapperLocations() {
        return mapperLocations;
    }

    public void setMapperLocations(Resource[] mapperLocations) {
        this.mapperLocations = mapperLocations;
    }

    public ExtConfiguration() {
        super();
    }

    public ExtConfiguration(Environment environment) {
        super(environment);
    }

    public void validateAllMapperMethod(boolean panicIfStatementNotFound) {
        for (Class<?> mapperInterface : mapperRegistry.getMappers()) {
            validateMapperMethod(mapperInterface, panicIfStatementNotFound);
        }
    }

    public void validateMapperMethod(Class<?> mapperInterface, boolean panicIfStatementNotFound) {
        for (Method method : mapperInterface.getDeclaredMethods()) {
            if (isBridgeOrDefault(method)) {
                continue;
            }
            String statementId = mapperInterface.getName() + "." + method.getName();
            MappedStatement ms = resolveMappedStatement(statementId);
            if (ms == null) {
                String message = "Invalid bound statement (not found): " + statementId;
                if (panicIfStatementNotFound) {
                    throw new BindingException(message);
                }
                logger.warn(message);
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
        if (!loadedStatementId.contains(id)) {
            if (!hasDefinedStatement(methodName, mapperInterface)) {
                return buildMappedStatement(id, methodName, mapperInterface);
            }
            loadedStatementId.add(id);
        }
        return null;
    }

    private boolean hasDefinedStatement(String methodName, Class<?> mapperInterface) {
        if (hasXmlDefinedStatement(methodName, mapperInterface)) {
            return true;
        }
        for (Method method : mapperInterface.getDeclaredMethods()) {
            if (isBridgeOrDefault(method)) {
                continue;
            }
            if (method.getName().equals(methodName) && hasStatementAnnotation(method)) {
                return true;
            }
        }
        return false;

    }

    private boolean hasStatementAnnotation(Method method) {
        return method.isAnnotationPresent(Lang.class) ||
                method.isAnnotationPresent(Select.class) ||
                method.isAnnotationPresent(Update.class) ||
                method.isAnnotationPresent(Delete.class) ||
                method.isAnnotationPresent(Insert.class) ||
                method.isAnnotationPresent(SelectProvider.class) ||
                method.isAnnotationPresent(UpdateProvider.class) ||
                method.isAnnotationPresent(DeleteProvider.class) ||
                method.isAnnotationPresent(InsertProvider.class);
    }

    private boolean hasXmlDefinedStatement(String methodName, Class<?> mapperInterface) {
        String xmlResource = mapperInterface.getName().replace('.', '/') + ".xml";
        InputStream inputStream = mapperInterface.getResourceAsStream("/" + xmlResource);
        if (inputStream == null) {
            try {
                inputStream = Resources.getResourceAsStream(mapperInterface.getClassLoader(), xmlResource);
            } catch (IOException e) {
                // ignore
            }
        }
        if (inputStream != null && hasXmlDefinedStatement(methodName, mapperInterface, inputStream)) {
            return true;
        }
        if (mapperLocations != null) {
            for (Resource mapperLocation : mapperLocations) {
                if (mapperLocation == null) {
                    continue;
                }
                try {
                    if (hasXmlDefinedStatement(methodName, mapperInterface, mapperLocation.getInputStream())) {
                        return true;
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return false;
    }

    private boolean hasXmlDefinedStatement(String methodName, Class<?> mapperInterface, InputStream inputStream) {
        XPathParser parser = new XPathParser(inputStream, false, null, new XMLMapperEntityResolver());
        XNode mapper = parser.evalNode("/mapper[@namespace='" + mapperInterface.getName() + "']");
        if (mapper == null) {
            return false;
        }
        XNode statement = mapper.evalNode("(select|insert|update|delete)[@id='" + methodName + "']");
        return statement != null;
    }

    private MappedStatement buildMappedStatement(String id, String methodName, Class<?> mapperInterface) {
        ExtMapper jpaMapper = mapperInterface.getAnnotation(ExtMapper.class);
        if (jpaMapper == null) {
            return null;
        }
        Class<?> tableType = jpaMapper.value();
        Class<?> returnType = null;
        List<Parameter[]> parametersList = new ArrayList<>();
        for (Method method : mapperInterface.getDeclaredMethods()) {
            if (isBridgeOrDefault(method)) {
                continue;
            }
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

    private boolean isBridgeOrDefault(Method method) {
        return method.isBridge() || method.isDefault();
    }

    private MappedStatement buildMappedStatement(String id, String methodName, List<Parameter[]> parametersList,
            Class<?> returnType, Class<?> tableType) {
        if (!"countDidaTask2".equals(methodName)) {
            return null;
        }
        // TODO
        // 考虑自己实现SqlSource
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
