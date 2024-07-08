package io.github.mybatisext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinParent {

    /** 别名 */
    String alias() default "";

    JoinColumn[] joinColumn();

    LoadType loadType() default LoadType.JOIN;
}
