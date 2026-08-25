package io.github.mybatisext.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.MapTable;
import io.github.mybatisext.mapper.ExtMapper;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.statement.MappedStatementHelper;
import io.github.mybatisext.util.TypeUtils;

public class ExtContextLoader {

    private static final Log log = LogFactory.getLog(ExtContextLoader.class);

    private final Configuration configuration;
    private final MappedStatementHelper mappedStatementHelper;

    public ExtContextLoader(Configuration configuration, ExtContext extContext) {
        this.configuration = configuration;
        this.mappedStatementHelper = new MappedStatementHelper(configuration, extContext);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void load() {
        for (Class<?> mapperClass : configuration.getMapperRegistry().getMappers()) {
            if (!isNotEnhancedMapper(mapperClass)) {
                loadMapper(mapperClass);
            }
        }
    }

    // 增量加载单个mapper
    public void load(Class<?> mapperClass) {
        if (!isNotEnhancedMapper(mapperClass) && configuration.hasMapper(mapperClass)) {
            loadMapper(mapperClass);
        }
    }

    // 每个mapper在自己命名空间生成全部可见方法，继承方法的解析由MyBatis沿接口层级向上完成。
    // 豁免：祖先命名空间已有用户自定义语句（注解/XML，带resource）时不复制，MyBatis解析时可沿层级找到；
    // 框架生成的语句无resource，则各命名空间各自生成，保证语句集合与加载顺序无关
    private void loadMapper(Class<?> mapperClass) {
        GenericType genericType = GenericTypeFactory.build(mapperClass);
        Map<String, List<GenericMethod>> nameToMethods = new LinkedHashMap<>();
        for (GenericMethod method : genericType.getMethods()) {
            if (method.isBridge() || method.isDefault()) {
                continue;
            }
            nameToMethods.computeIfAbsent(method.getName(), k -> new ArrayList<>()).add(method);
        }
        for (Map.Entry<String, List<GenericMethod>> entry : nameToMethods.entrySet()) {
            String methodName = entry.getKey();
            String id = mapperClass.getName() + "." + methodName;
            if (configuration.hasStatement(id)) {
                continue;
            }
            MappedStatement inherited = resolveInheritedMappedStatement(mapperClass, methodName);
            if (inherited != null && inherited.getResource() != null) {
                continue;
            }
            MappedStatement ms = buildMappedStatement(id, mapperClass, entry.getValue());
            if (ms == null) {
                throw new BindingException("Invalid bound statement (not found): " + id);
            }
            try {
                configuration.addMappedStatement(ms);
            } catch (IllegalArgumentException e) {
                log.error("MappedStatement already registered: " + ms.getId(), e);
            }
        }
    }

    // 沿接口层级向上查找已存在的statement（不含自身命名空间）
    private MappedStatement resolveInheritedMappedStatement(Class<?> mapperClass, String methodName) {
        for (Class<?> superInterface : mapperClass.getInterfaces()) {
            String statementId = superInterface.getName() + "." + methodName;
            if (configuration.hasStatement(statementId)) {
                return configuration.getMappedStatement(statementId);
            }
            MappedStatement ms = resolveInheritedMappedStatement(superInterface, methodName);
            if (ms != null) {
                return ms;
            }
        }
        return null;
    }

    private MappedStatement buildMappedStatement(String id, Class<?> mapperClass, List<GenericMethod> methods) {
        GenericType returnType = null;
        for (GenericMethod method : methods) {
            GenericType mReturnType = TypeUtils.unwrapToGenericType(method.getGenericReturnType());
            if (returnType == null || returnType.isAssignableFrom(mReturnType)) {
                returnType = mReturnType;
            } else if (!mReturnType.isAssignableFrom(returnType) && mReturnType.getType() != Void.class) {
                throw new IllegalArgumentException("returnType inconsistency: " + mapperClass.getName() + "." + methods.get(0).getName());
            }
        }
        return mappedStatementHelper.build(id, getEntityClass(mapperClass), methods, returnType);
    }

    private boolean isNotEnhancedMapper(Class<?> mapperClass) {
        return !mapperClass.isInterface() || mapperClass.getTypeParameters().length > 0 || (!mapperClass.isAnnotationPresent(MapTable.class) && !ExtMapper.class.isAssignableFrom(mapperClass));
    }

    private GenericType getEntityClass(Class<?> mapperClass) {
        MapTable annotation = mapperClass.getAnnotation(MapTable.class);
        if (annotation != null) {
            return GenericTypeFactory.build(annotation.value());
        }
        return TypeUtils.resolveGenericTypeArgument(mapperClass, ExtMapper.class, 0);
    }

}
