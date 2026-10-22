package io.github.mybatisext.model;

/** JavaBeans getter 的选择由源码适配器负责；字段与这些属性之间的映射优先级由元数据解析器决定。 */
public final class JavaBeanProperty {

    private final String name;
    private final JavaMethod readMethod;
    private final boolean readonly;

    public JavaBeanProperty(String name, JavaMethod readMethod, boolean readonly) {
        this.name = name;
        this.readMethod = readMethod;
        this.readonly = readonly;
    }

    public String getName() {
        return name;
    }

    public JavaMethod getReadMethod() {
        return readMethod;
    }

    public boolean isReadonly() {
        return readonly;
    }
}
