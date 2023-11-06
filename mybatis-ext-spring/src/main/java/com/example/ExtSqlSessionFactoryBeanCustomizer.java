package com.example;

import java.util.List;

import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.mybatis.spring.boot.autoconfigure.SqlSessionFactoryBeanCustomizer;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

// https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/zh/index.html#使用-sqlsessionfactorybeancustomizer
public class ExtSqlSessionFactoryBeanCustomizer implements SqlSessionFactoryBeanCustomizer {

    private final MybatisProperties properties;
    private final List<ConfigurationCustomizer> configurationCustomizers;

    public ExtSqlSessionFactoryBeanCustomizer(MybatisProperties properties,
            List<ConfigurationCustomizer> configurationCustomizers) {
        this.properties = properties;
        this.configurationCustomizers = configurationCustomizers;
    }

    @Override
    public void customize(SqlSessionFactoryBean factoryBean) {
        applyConfiguration(factoryBean);
    }

    private void applyConfiguration(SqlSessionFactoryBean factory) {
        Configuration configuration = this.properties.getConfiguration();
        if (configuration == null && !StringUtils.hasText(this.properties.getConfigLocation())) {
            configuration = new Configuration();
        }
        if (configuration != null && !CollectionUtils.isEmpty(this.configurationCustomizers)) {
            for (ConfigurationCustomizer customizer : this.configurationCustomizers) {
                customizer.customize(configuration);
            }
        }
        ExtConfiguration jpaConfiguration = new ExtConfiguration();
        BeanUtils.copyProperties(configuration, jpaConfiguration);
        jpaConfiguration.setMapperLocations(this.properties.resolveMapperLocations());
        factory.setConfiguration(jpaConfiguration);
    }

}
