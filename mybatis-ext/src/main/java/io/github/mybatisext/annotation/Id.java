package io.github.mybatisext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.mybatisext.idgenerator.IdGenerator;

@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
    interface DefaultIdGenerator extends IdGenerator<Object> {
    }

    IdType idType() default IdType.NONE;

    Class<? extends IdGenerator<?>> customIdGenerator() default DefaultIdGenerator.class;
}
