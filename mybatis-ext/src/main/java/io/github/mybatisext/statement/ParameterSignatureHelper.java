package io.github.mybatisext.statement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.reflection.ParamNameUtil;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericParameter;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.util.CommonUtils;

public class ParameterSignatureHelper {

    public static ParameterSignature buildParameterSignature(Configuration configuration, GenericMethod method) {
        Map<String, GenericType> nameToType = buildNameToType(configuration, method);
        if (nameToType.size() == 1) {
            Map.Entry<String, GenericType> entry = nameToType.entrySet().iterator().next();
            return buildSignatureIfCollection(entry.getValue().getType(), entry.getKey());
        }
        ParameterSignature parameterSignature = new ParameterSignature();
        parameterSignature.setType(MapperMethod.ParamMap.class);
        for (Map.Entry<String, GenericType> entry : nameToType.entrySet()) {
            parameterSignature.getNameToType().put(entry.getKey(), entry.getValue().getType());
        }
        return parameterSignature;
    }

    public static boolean isParameterSignatureMatch(Object object, String s) {
        return isParameterSignatureMatch(object, fromString(s));
    }

    public static boolean isParameterSignatureMatch(Object object, ParameterSignature parameterSignature) {
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
        String s = parameterSignature.getType().getName();
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

    private static Map<String, GenericType> buildNameToType(Configuration configuration, GenericMethod method) {
        GenericParameter[] parameters = method.getParameters();
        Map<String, GenericType> nameToType = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            if (CommonUtils.isSpecialParameter(parameters[i].getType())) {
                continue;
            }
            String name = null;
            Param param = parameters[i].getAnnotation(Param.class);
            if (param != null) {
                name = param.value();
            } else {
                if (configuration.isUseActualParamName()) {
                    name = getActualParamName(method.getMethod(), i);
                }
                if (name == null) {
                    name = String.valueOf(nameToType.size());
                }
            }
            nameToType.put(name, parameters[i].getGenericType());
        }
        return nameToType;
    }

    private static String getActualParamName(Method method, int paramIndex) {
        return ParamNameUtil.getParamNames(method).get(paramIndex);
    }

    private static ParameterSignature buildSignatureIfCollection(Class<?> type, String actualParamName) {
        ParameterSignature parameterSignature = new ParameterSignature();
        if (Collection.class.isAssignableFrom(type)) {
            parameterSignature.setType(MapperMethod.ParamMap.class);
            parameterSignature.getNameToType().put("collection", type);
            if (List.class.isAssignableFrom(type)) {
                parameterSignature.getNameToType().put("list", type);
            }
            if (actualParamName != null) {
                parameterSignature.getNameToType().put(actualParamName, type);
            }
            return parameterSignature;
        }
        if (type.isArray()) {
            parameterSignature.setType(MapperMethod.ParamMap.class);
            parameterSignature.getNameToType().put("array", type);
            if (actualParamName != null) {
                parameterSignature.getNameToType().put(actualParamName, type);
            }
            return parameterSignature;
        }
        parameterSignature.setType(type);
        return parameterSignature;
    }
}
