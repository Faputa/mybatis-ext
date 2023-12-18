package io.github.mybatisext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Repeatable(JoinRelations.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinRelation {

    JoinColumn[] joinColumn();

    Class<?> table() default void.class;

    String tableAlias() default "";

    String column() default "";
}
