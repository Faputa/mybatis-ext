package com.example;

import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExtConfigurationCustomizer implements ConfigurationCustomizer {

    @Autowired
    private MybatisProperties mybatisProperties;

    @Override
    public void customize(Configuration configuration) {
        if (configuration instanceof ExtConfiguration) {
            ((ExtConfiguration) configuration).setMapperLocations(mybatisProperties.resolveMapperLocations());
        }
    }

}
