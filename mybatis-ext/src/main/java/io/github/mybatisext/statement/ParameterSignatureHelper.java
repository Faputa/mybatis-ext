package io.github.mybatisext.statement;

import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericParameter;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.Configuration;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParameterSignatureHelper {
    
    public static String buildSignature(Configuration configuration, GenericMethod method) {
        ParamNameResolver paramNameResolver = new ParamNameResolver(configuration, method.getMethod());
        String[] names = paramNameResolver.getNames();
        if (names.length == 0) {
            return null;
        }
        return "";
    }

    // public Object getNamedParams(Object[] args) {
    //     final int paramCount = names.size();
    //     if (args == null || paramCount == 0) {
    //         return null;
    //     }
    //     if (!hasParamAnnotation && paramCount == 1) {
    //         Object value = args[names.firstKey()];
    //         return wrapToMapIfCollection(value, useActualParamName ? names.get(names.firstKey()) : null);
    //     } else {
    //         final Map<String, Object> param = new MapperMethod.ParamMap<>();
    //         int i = 0;
    //         for (Map.Entry<Integer, String> entry : names.entrySet()) {
    //             param.put(entry.getValue(), args[entry.getKey()]);
    //             // add generic param names (param1, param2, ...)
    //             final String genericParamName = GENERIC_NAME_PREFIX + (i + 1);
    //             // ensure not to overwrite parameter named with @Param
    //             if (!names.containsValue(genericParamName)) {
    //                 param.put(genericParamName, args[entry.getKey()]);
    //             }
    //             i++;
    //         }
    //         return param;
    //     }
    // }

    private static Object wrapToMapIfCollection(Object object, String actualParamName) {
        if (object instanceof Collection) {
            MapperMethod.ParamMap<Object> map = new MapperMethod.ParamMap<>();
            map.put("collection", object);
            if (object instanceof List) {
                map.put("list", object);
            }
            Optional.ofNullable(actualParamName).ifPresent(name -> map.put(name, object));
            return map;
        }
        if (object != null && object.getClass().isArray()) {
            MapperMethod.ParamMap<Object> map = new MapperMethod.ParamMap<>();
            map.put("array", object);
            Optional.ofNullable(actualParamName).ifPresent(name -> map.put(name, object));
            return map;
        }
        return object;
    }
}
