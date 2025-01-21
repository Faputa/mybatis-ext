package io.github.mybatisext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.mybatisext.jpa.CompareOperator;
import io.github.mybatisext.jpa.LogicalOperator;

@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Filterable {

    IfTest test() default IfTest.NotNull;

    CompareOperator operator() default CompareOperator.Equals;

    LogicalOperator logicalOperator() default LogicalOperator.AND;

    boolean ignorecase() default false;

    boolean not() default false;

    String testTemplate() default "";

    String exprTemplate() default "";

    String secondVariable() default "";
}
