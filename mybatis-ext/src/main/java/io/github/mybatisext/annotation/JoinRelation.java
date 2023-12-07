package io.github.mybatisext.annotation;

import java.lang.annotation.Repeatable;

@Repeatable(JoinRelations.class)
public @interface JoinRelation {

    JoinColumn[] joinColumn();

    Class<?> table() default void.class;

    String tableAlias() default "";

    String column() default "";

    // Java不允许定义递归注解，此路不通！！
    // https://stackoverflow.com/questions/12296452/java-annotation-recursive-dependency
    // JoinRelation joinRelation() default null;
}
