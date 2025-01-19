package io.github.mybatisext.spring;

import java.util.Map;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

import io.github.mybatisext.adapter.ExtConfiguration;

public class MapperMethodValidator implements SmartInitializingSingleton {

    private final Map<String, SqlSessionFactory> sqlSessionFactoryMap;

    public MapperMethodValidator(Map<String, SqlSessionFactory> sqlSessionFactoryMap) {
        this.sqlSessionFactoryMap = sqlSessionFactoryMap;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryMap.values()) {
            Configuration configuration = sqlSessionFactory.getConfiguration();
            if (configuration instanceof ExtConfiguration) {
                ExtConfiguration extConfiguration = (ExtConfiguration) configuration;
                extConfiguration.validateAllMapperMethod();
            }
        }
    }
}
