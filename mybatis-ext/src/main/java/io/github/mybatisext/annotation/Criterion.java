package io.github.mybatisext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.mybatisext.jpa.ConditionRel;
import io.github.mybatisext.jpa.IfTest;

@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Criterion {

    IfTest test() default IfTest.None;

    ConditionRel rel() default ConditionRel.Equals;

    String property() default "";

    String secondVariable() default "";

    boolean ignorecase() default false;

    boolean not() default false;
}
