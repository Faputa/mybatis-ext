package io.github.mybatisext.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RelatedOn {

    /** 本地字段 */
    String column();

    /** 远端字段 */
    String relatedColumn();
}
