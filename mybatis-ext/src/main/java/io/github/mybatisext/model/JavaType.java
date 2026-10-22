package io.github.mybatisext.model;

/** 成员及父类型必须已在当前类型的泛型上下文中完成替换。
 * 相等性判断必须包含泛型实参及所属的解析环境。
 * 实现不得跨分析会话保留失效的 IDE 元素。 */
public interface JavaType extends JavaAnnotatedElement {

    String getName();

    String getSimpleName();

    JavaType[] getTypeParameters();

    JavaType getComponentType();

    JavaField[] getDeclaredFields();

    JavaMethod[] getMethods();

    JavaType getGenericSuperclass();

    JavaType[] getGenericInterfaces();

    JavaBeanProperty[] getBeanProperties();

    boolean isInterface();

    default boolean isArray() {
        return getComponentType() != null;
    }

    /** 符号未解析、类型变量未替换或通配符不受支持时，返回 false。 */
    default boolean isResolved() {
        return true;
    }

    default String getTypeName() {
        return getName();
    }
}
