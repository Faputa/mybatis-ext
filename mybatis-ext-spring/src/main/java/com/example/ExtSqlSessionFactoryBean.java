package com.example;

import java.util.Arrays;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.Resource;

public class ExtSqlSessionFactoryBean extends SqlSessionFactoryBean {

    private Resource[] mapperLocations;
    private SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();

    public void setMapperLocations(Resource... mapperLocations) {
        this.mapperLocations = mapperLocations;
        super.setMapperLocations(mapperLocations);
    }

    public void addMapperLocations(Resource... mapperLocations) {
        Resource[] appendArrays = appendArrays(this.mapperLocations, mapperLocations);
        this.mapperLocations = appendArrays;
        super.addMapperLocations(appendArrays);
    }

    protected SqlSessionFactory buildSqlSessionFactory() throws Exception {
        SqlSessionFactory sqlSessionFactory = super.buildSqlSessionFactory();
        Configuration configuration = sqlSessionFactory.getConfiguration();
        if (configuration instanceof ExtConfiguration) {
            return sqlSessionFactory;
        }
        ExtConfiguration extConfiguration = new ExtConfiguration(configuration);
        extConfiguration.setMapperLocations(mapperLocations);
        extConfiguration.validateAllMapperMethod(false);
        return sqlSessionFactoryBuilder.build(extConfiguration);
    }

    private <T> T[] appendArrays(T[] left, T[] right) {
        T[] dist = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, dist, left.length, right.length);
        return dist;
    }

}
