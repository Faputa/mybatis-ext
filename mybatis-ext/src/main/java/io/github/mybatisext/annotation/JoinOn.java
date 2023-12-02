package io.github.mybatisext.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JoinOn {

    /** 本地字段 */
    String column();

    /** 关联字段 */
    String joinColumn();
}
