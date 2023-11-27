package io.github.mybatisext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    /** 字段名，默认为属性名小写转下划线 */
    String name() default "";

    /** 属性，默认为属性名 */
    String property() default "";

    /** 注释 */
    String comment() default "";
}
