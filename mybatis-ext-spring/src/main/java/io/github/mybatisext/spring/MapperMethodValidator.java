package io.github.mybatisext.spring;

import java.util.Collection;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

import io.github.mybatisext.adapter.ConfigurationInterface;

public class MapperMethodValidator implements SmartInitializingSingleton {

    private final Collection<SqlSessionFactory> sqlSessionFactories;

    public MapperMethodValidator(Collection<SqlSessionFactory> sqlSessionFactories) {
        this.sqlSessionFactories = sqlSessionFactories;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactories) {
            Configuration configuration = sqlSessionFactory.getConfiguration();
            if (configuration instanceof ConfigurationInterface) {
                ConfigurationInterface configurationInterface = (ConfigurationInterface) configuration;
                configurationInterface.validateAllMapperMethod();
            }
        }
    }
}
