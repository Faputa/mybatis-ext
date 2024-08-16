package io.github.mybatisext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.mybatisext.jpa.ConditionListRel;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Criteria {

    Class<?> table() default void.class;

    ConditionListRel rel() default ConditionListRel.And;
}
