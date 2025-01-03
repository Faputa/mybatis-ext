package io.github.mybatisext.ognl;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

public class Ognl {

    public static final String IsEmpty = "@io.github.mybatisext.ognl.Ognl@isEmpty";
    public static final String IsNotEmpty = "@io.github.mybatisext.ognl.Ognl@isNotEmpty";
    public static final String IsNumber = "@io.github.mybatisext.ognl.Ognl@isNumber";
    public static final String IsNotNumber = "@io.github.mybatisext.ognl.Ognl@isNotNumber";
    public static final String HasProperty = "@io.github.mybatisext.ognl.Ognl@hasProperty";
    public static final String ToUpperCase = "@io.github.mybatisext.ognl.Ognl@toUpperCase";
    public static final String IsParameterSignatureMatch = "@io.github.mybatisext.ognl.Ognl@isParameterSignatureMatch";

    /**
     * 可以用于判断String,Long,Integer,Map,Array,Collection是否为空
     *
     * @param o Object
     * @return
     */
    public static boolean isEmpty(Object o) {
        if (o == null) {
            return true;
        }
        if (o instanceof String && ((String) o).trim().isEmpty()) {
            return true;
        }
        if (o instanceof Map && ((Map<?, ?>) o).isEmpty()) {
            return true;
        }
        if (o.getClass().isArray() && ((Object[]) o).length == 0) {
            return true;
        }
        if (o instanceof Collection && ((Collection<?>) o).isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * 可以用于判断String,Long,Integer,Map,Array,Collection是否不为空
     *
     * @param o Object
     * @return
     */
    public static boolean isNotEmpty(Object o) {
        return !isEmpty(o);
    }

    /**
     * 可以用于判断是否为数值类型
     *
     * @param o Object
     * @return
     */
    public static boolean isNumber(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Number) {
            return true;
        }
        if (o instanceof String) {
            try {
                Double.parseDouble((String) o);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 可以用于判断是否不为数值类型
     *
     * @param o Object
     * @return
     */
    public static boolean isNotNumber(Object o) {
        return !isNumber(o);
    }

    /**
     * 判断对象是否存在属性
     *
     * @param o            对象
     * @param propertyName 属性名
     * @return
     * @throws IntrospectionException
     */
    public static boolean hasProperty(Object o, String propertyName) throws IntrospectionException {
        if (o == null) {
            return false;
        }
        if (o instanceof Map) {
            return true;
        }
        BeanInfo beanInfo = Introspector.getBeanInfo(o.getClass());
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            if (propertyDescriptor.getName().equals(propertyName)) {
                Method readMethod = propertyDescriptor.getReadMethod();
                if (readMethod != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 将对象转字符串再转大写
     *
     * @param o 对象
     * @return
     */
    public static String toUpperCase(Object o) {
        return o.toString().toUpperCase();
    }

    /**
     * 校验参数是否匹配
     * @param _parameter 参数对象
     * @param signature 签名
     * @return 是否匹配
     */
    public static boolean isParameterSignatureMatch(Object _parameter, String signature) {
        // TODO
        return false;
    }
}
