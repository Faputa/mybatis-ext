package io.github.mybatisext.annotation;

public @interface JoinColumn {

    String leftTableAlias() default "";

    String leftColumn();

    String rightColumn();
}
