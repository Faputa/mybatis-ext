package io.github.mybatisext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ibatis.mapping.FetchType;

@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinColumn {

    /** 字段名，默认为属性名小写转下划线 */
    String name() default "";

    /** 属性，默认为属性名 */
    String property() default "";

    /** 获取类型，默认连表查询 */
    FetchType fetchType() default FetchType.DEFAULT;

    /** 注释 */
    String comment() default "";

    /** 关联表 */
    Class<?> tableClass();

    /** 关联列，默认为属性名小写转下划线 */
    String referencedColumnName() default "";
}
