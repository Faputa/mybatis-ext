package io.github.mybatisext.statement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.ParamNameUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;

public class ParameterSignatureHelper {

    // org.apache.ibatis.reflection.ParamNameResolver
    public static ParameterSignature buildParameterSignature(Configuration config, GenericMethod method) {
        boolean useActualParamName = config.isUseActualParamName();
        boolean hasParamAnnotation = false;
        GenericType[] paramTypes = method.getGenericParameterTypes();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        SortedMap<Integer, String> map = new TreeMap<>();
        int paramCount = paramAnnotations.length;
        for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
            if (isSpecialParameter(paramTypes[paramIndex].getType())) {
                continue;
            }
            String name = null;
            for (Annotation annotation : paramAnnotations[paramIndex]) {
                if (annotation instanceof Param) {
                    hasParamAnnotation = true;
                    name = ((Param) annotation).value();
                    break;
                }
            }
            if (name == null) {
                if (useActualParamName) {
                    name = getActualParamName(method.getMethod(), paramIndex);
                }
                if (name == null) {
                    name = String.valueOf(map.size());
                }
            }
            map.put(paramIndex, name);
        }
        SortedMap<Integer, String> names = Collections.unmodifiableSortedMap(map);
        return getNamedParams(method.getGenericParameterTypes(), names, useActualParamName, hasParamAnnotation);
    }

    private static boolean isSpecialParameter(Class<?> clazz) {
        return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
    }

    public static ParameterSignature getNamedParams(GenericType[] parameterTypes, SortedMap<Integer, String> names, boolean useActualParamName, boolean hasParamAnnotation) {
        int paramCount = names.size();
        if (parameterTypes == null || paramCount == 0) {
            return new ParameterSignature();
        }
        if (!hasParamAnnotation && paramCount == 1) {
            GenericType genericType = parameterTypes[names.firstKey()];
            return wrapToMapIfCollection(genericType.getType(), useActualParamName ? names.get(names.firstKey()) : null);
        } else {
            ParameterSignature parameterSignature = new ParameterSignature();
            parameterSignature.setType(MapperMethod.ParamMap.class);
            int i = 0;
            for (Map.Entry<Integer, String> entry : names.entrySet()) {
                parameterSignature.getNameToType().put(entry.getValue(), parameterTypes[entry.getKey()].getType());
                String genericParamName = ParamNameResolver.GENERIC_NAME_PREFIX + (i + 1);
                if (!names.containsValue(genericParamName)) {
                    parameterSignature.getNameToType().put(genericParamName, parameterTypes[entry.getKey()].getType());
                }
                i++;
            }
            return parameterSignature;
        }
    }

    public static ParameterSignature wrapToMapIfCollection(Class<?> object, String actualParamName) {
        ParameterSignature parameterSignature = new ParameterSignature();
        if (Collection.class.isAssignableFrom(object)) {
            parameterSignature.setType(MapperMethod.ParamMap.class);
            parameterSignature.getNameToType().put("collection", object);
            if (List.class.isAssignableFrom(object)) {
                parameterSignature.getNameToType().put("list", object);
            }
            Optional.ofNullable(actualParamName).ifPresent(name -> parameterSignature.getNameToType().put(name, object));
            return parameterSignature;
        }
        if (object.isArray()) {
            parameterSignature.setType(MapperMethod.ParamMap.class);
            parameterSignature.getNameToType().put("array", object);
            Optional.ofNullable(actualParamName).ifPresent(name -> parameterSignature.getNameToType().put(name, object));
            return parameterSignature;
        }
        parameterSignature.setType(object);
        return parameterSignature;
    }

    public static boolean isParameterSignatureMatch(Object object, String s) {
        return isParameterSignatureMatch(object, fromString(s));
    }

    public static boolean isParameterSignatureMatch(Object object, ParameterSignature parameterSignature) {
        if (object == null) {
            return parameterSignature.getType() == null;
        }
        if (object instanceof MapperMethod.ParamMap) {
            MapperMethod.ParamMap<?> paramMap = (MapperMethod.ParamMap<?>) object;
            if (paramMap.size() != parameterSignature.getNameToType().size()) {
                return false;
            }
            for (Map.Entry<String, Class<?>> entry : parameterSignature.getNameToType().entrySet()) {
                if (!paramMap.containsKey(entry.getKey())) {
                    return false;
                }
                Object value = paramMap.get(entry.getKey());
                if (value != null && !entry.getValue().isInstance(value)) {
                    return false;
                }
            }
            return true;
        }
        return parameterSignature.getType().isInstance(object);
    }

    public static String toString(ParameterSignature parameterSignature) {
        String s = parameterSignature.getType() != null ? parameterSignature.getType().getName() : "";
        List<String> ss = new ArrayList<>();
        for (Map.Entry<String, Class<?>> entry : parameterSignature.getNameToType().entrySet()) {
            ss.add(entry.getKey() + ":" + entry.getValue().getName());
        }
        return s + "|" + String.join(",", ss);
    }

    public static ParameterSignature fromString(String s) {
        String[] ss = s.split("\\|", -1);
        if (ss.length != 2) {
            throw new MybatisExtException("Invalid input format: Expected exactly one '|' separator.");
        }
        try {
            ParameterSignature parameterSignature = new ParameterSignature();
            parameterSignature.setType(Class.forName(ss[0]));
            if (ss[1].isEmpty()) {
                return parameterSignature;
            }
            String[] pairs = ss[1].split(",");
            for (String pair : pairs) {
                String[] split = pair.split(":");
                if (split.length != 2) {
                    throw new MybatisExtException("Invalid parameter format: Expected 'name:class' pairs separated by commas.");
                }
                parameterSignature.getNameToType().put(split[0], Class.forName(split[1]));
            }
            return parameterSignature;
        } catch (ClassNotFoundException e) {
            throw new MybatisExtException(e);
        }
    }

    private static String getActualParamName(Method method, int paramIndex) {
        return ParamNameUtil.getParamNames(method).get(paramIndex);
    }
}
