package com.mybatisext.spring.boot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@ComponentScan
@EnableAspectJAutoProxy
@ConditionalOnProperty(prefix = MybatisExtProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisExtAutoConfiguration {

}
