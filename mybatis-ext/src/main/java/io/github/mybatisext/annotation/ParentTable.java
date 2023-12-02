package io.github.mybatisext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Repeatable(ParentTables.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParentTable {

    /** 关联表的实体类 */
    Class<?> tableClass();

    /** 关联字段 */
    JoinOn[] joinOn();
}
