package com.example;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MybatisExtAspect {

    @Autowired
    private MybatisProperties properties;
    private SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();

    @Around("@annotation(org.springframework.context.annotation.Bean)")
    public Object aroundBeanMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (result instanceof SqlSessionFactory) {
            SqlSessionFactory sqlSessionFactory = (SqlSessionFactory) result;
            Configuration configuration = sqlSessionFactory.getConfiguration();
            if (configuration instanceof ExtConfiguration) {
                return result;
            }
            ExtConfiguration extConfiguration = new ExtConfiguration(configuration);
            extConfiguration.setMapperLocations(properties.resolveMapperLocations());
            extConfiguration.validateAllMapperMethod(false);
            return sqlSessionFactoryBuilder.build(extConfiguration);
        }
        return result;
    }

}
