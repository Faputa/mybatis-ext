package io.github.mybatisext.spring.boot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.github.mybatisext.dialect.DefaultDialectSelector;
import io.github.mybatisext.dialect.DialectSelector;

@ComponentScan
@ConditionalOnProperty(prefix = MybatisExtProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class MybatisExtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    DialectSelector dialectSelector() {
        return new DefaultDialectSelector();
    }
}
