package io.github.mybatisext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.mybatisext.jpa.CompareOperator;

@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterSpec {

    IfTest test() default IfTest.NotNull;

    CompareOperator operator() default CompareOperator.Equals;

    String testTemplate() default "";

    String exprTemplate() default "";

    String secondVariable() default "";
}
