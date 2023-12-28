package io.github.mybatisext.annotation;

public enum IdType {

    /** 数据库自增 */
    AUTO,
    /** UUID */
    UUID,
    /** 自定义 @see {@link io.github.mybatisext.idgenerator.IdGenerator} */
    CUSTOM,
    /** 无 */
    NONE
}
