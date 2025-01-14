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
        return build(template, param, true);
    }

    public static String build(String template, Object param, boolean useStrict) {
        StringBuilder sb = new StringBuilder();
        StringBuilder placeholder = new StringBuilder();
        StringBuilder key = new StringBuilder();
        List<String> keys = new ArrayList<>();
        boolean inGroup = false;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '\\') {
                // 转义字符
                i++;
                if (i < template.length()) {
                    char c1 = template.charAt(i);
                    if (inGroup) {
                        placeholder.append(c1);
                        key.append(c1);
                    } else {
                        sb.append(c1);
                    }
                }
            } else if (c == '{') {
                // 支持{嵌套
                sb.append(placeholder);
                key.setLength(0);
                keys.clear();
                placeholder.setLength(0);
                placeholder.append(c);
                inGroup = true;
            } else if (inGroup) {
                placeholder.append(c);
                if (c == '.') {
                    keys.add(key.toString().trim());
                    key.setLength(0);
                } else if (c == '}') {
                    keys.add(key.toString().trim());
                    Object obj = deepGet(param, keys);
                    if (obj == null) {
                        if (useStrict) {
                            throw new IllegalArgumentException("param path not found: " + String.join(".", keys));
                        }
                        sb.append(placeholder);
                    } else {
                        sb.append(obj);
                    }
                    placeholder.setLength(0);
                    inGroup = false;
                } else {
                    key.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        sb.append(placeholder);
        return sb.toString();
    }

    /**
     * like lodash get
     */
    private static Object deepGet(Object obj, List<String> keys) {
        for (String key : keys) {
            if (obj == null) {
                return null;
            }
            if (key.startsWith("##")) {
                // 强制##开头的名字为对象属性
                obj = getProperty(obj, key.substring(2));
            } else if (obj instanceof Map) {
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
                obj = getProperty(obj, key);
            }
        }
        return obj;
    }

    private static Object getProperty(Object obj, String key) {
        try {
            Method readMethod = getReadMethod(obj.getClass(), key);
            if (readMethod == null) {
                return null;
            }
            return readMethod.invoke(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
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
