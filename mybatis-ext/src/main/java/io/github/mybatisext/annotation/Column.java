package io.github.mybatisext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ibatis.type.JdbcType;

@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    /** 字段名，默认为属性名小写转下划线 */
    String name() default "";

    /** 注释 */
    String comment() default "";

    /** 是否可空 */
    boolean nullable() default true;

    /** DDL语句 */
    String columnDefinition() default "";

    /** 长度 */
    int length() default 255;

    /** 精度 */
    int precision() default 0;

    /** 标度 */
    int scale() default 0;

    /** JDBC类型 */
    JdbcType jdbcType() default JdbcType.UNDEFINED;
}
