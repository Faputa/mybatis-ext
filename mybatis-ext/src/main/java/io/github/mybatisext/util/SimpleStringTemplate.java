package io.github.mybatisext.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleStringTemplate {

    public static String build(String template, Object param) {
        StringBuilder sb = new StringBuilder();
        StringBuilder group = new StringBuilder();
        StringBuilder key = new StringBuilder();
        List<String> keys = new ArrayList<>();
        boolean inGroup = false;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '{') {
                sb.append(group);
                key.setLength(0);
                keys.clear();
                group.setLength(0);
                group.append(c);
                inGroup = true;
            } else if (inGroup) {
                group.append(c);
                if (c == '.') {
                    keys.add(key.toString().trim());
                    key.setLength(0);
                } else if (c == '}') {
                    keys.add(key.toString().trim());
                    Object obj = deepGet(param, keys);
                    sb.append(obj != null ? obj.toString() : group);
                    group.setLength(0);
                    inGroup = false;
                } else {
                    key.append(c);
                }
            } else if (c == '\\') {
                i++;
                if (i < template.length()) {
                    sb.append(template.charAt(i));
                }
            } else {
                sb.append(c);
            }
        }
        sb.append(group);
        return sb.toString();
    }

    /** like lodash get */
    private static Object deepGet(Object obj, List<String> keys) {
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
                    throw new IllegalArgumentException(e);
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
