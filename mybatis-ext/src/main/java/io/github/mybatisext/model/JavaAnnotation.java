package io.github.mybatisext.model;

/** 提供包含默认值的有效注解值。类值表示为 JavaType，嵌套注解数组表示为 JavaAnnotation 数组，不加载应用类。 */
public interface JavaAnnotation {

    <T> T value(String name);
}
