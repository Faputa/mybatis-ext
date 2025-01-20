package io.github.mybatisext.spring.boot;

import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.dialect.DefaultDialectSelector;
import io.github.mybatisext.dialect.DialectSelector;
import io.github.mybatisext.spring.MapperMethodValidator;
import io.github.mybatisext.spring.MybatisExtBeanPostProcessor;

@ConditionalOnProperty(prefix = MybatisExtProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class MybatisExtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MybatisExtProperties mybatisExtProperties() {
        return new MybatisExtProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    DialectSelector mybatisExtDialectSelector() {
        return new DefaultDialectSelector();
    }

    @Bean
    @ConditionalOnMissingBean
    ExtContext mybatisExtContext(MybatisExtProperties properties, DialectSelector dialectSelector) {
        ExtContext extContext = new ExtContext();
        extContext.setDefaultFilterable(properties.isDefaultFilterable());
        extContext.setDialectSelector(dialectSelector);
        return extContext;
    }

    @Bean
    @ConditionalOnMissingBean
    MybatisExtBeanPostProcessor mybatisExtBeanPostProcessor(ExtContext extContext) {
        MybatisExtBeanPostProcessor mybatisExtBeanPostProcessor = new MybatisExtBeanPostProcessor();
        mybatisExtBeanPostProcessor.setExtContext(extContext);
        return mybatisExtBeanPostProcessor;
    }

    @Bean
    @ConditionalOnMissingBean
    MapperMethodValidator mybatisExtMapperMethodValidator(Map<String, SqlSessionFactory> sqlSessionFactoryMap) {
        return new MapperMethodValidator(sqlSessionFactoryMap);
    }
}
