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
public @interface JoinColumns {

    /** 关联列 */
    JoinColumn[] value();

    /** 获取类型，默认连表查询 */
    FetchType fetchType() default FetchType.DEFAULT;
}
