package com.example;


import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class ExtMybatisProperties extends MybatisProperties {

    @NestedConfigurationProperty
    private ExtConfiguration configuration = new ExtConfiguration();

    public ExtConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ExtConfiguration configuration) {
        this.configuration = configuration;
    }
}
