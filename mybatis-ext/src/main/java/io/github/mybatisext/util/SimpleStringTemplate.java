package io.github.mybatisext.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleStringTemplate {

    public static String build(String template, Object param) {
        Matcher matcher = Pattern.compile("\\{([^{}]+?)\\}").matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String path = matcher.group(1);
            Object obj = deepGet(param, path);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(obj != null ? obj.toString() : matcher.group()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /** like lodash get */
    private static Object deepGet(Object obj, String path) {
        String[] keys = path.trim().split("\\s*\\.\\s*");
        for (String key : keys) {
            if (obj == null) {
                return null;
            }
            if (obj instanceof Map) {
                obj = ((Map<?, ?>) obj).get(key);
            } else if (isInt(key)) {
                int idx = Integer.parseInt(key);
                if (obj instanceof List) {
                    if (idx < 0 || idx >= ((List<?>) obj).size()) {
                        return null;
                    }
                    obj = ((List<?>) obj).get(idx);
                } else if (obj.getClass().isArray()) {
                    if (idx < 0 || idx >= ((Object[]) obj).length) {
                        return null;
                    }
                    obj = ((Object[]) obj)[idx];
                } else {
                    return null;
                }
            } else {
                try {
                    Method readMethod = getReadMethod(obj.getClass(), key);
                    if (readMethod == null) {
                        return null;
                    }
                    obj = readMethod.invoke(obj);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return obj;
    }

    private static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static Method getReadMethod(Class<?> beanClass, String propertyName) throws IntrospectionException {
        // https://stackoverflow.com/questions/2638590/best-way-of-invoking-getter-by-reflection
        BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            if (propertyDescriptor.getName().equals(propertyName)) {
                return propertyDescriptor.getReadMethod();
            }
        }
        return null;
    }
}
