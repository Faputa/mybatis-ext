package io.github.mybatisext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {

    /** 表名，默认类名小写转下划线 */
    String name() default "";

    /** 注释 */
    String comment() default "";

    /** 模式名 */
    String schema() default "";
}
