package com.mybatisext.spring.boot;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.mybatisext.ExtConfiguration;

@Aspect
@Component
public class MybatisExtAspect {

    private final SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();

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
            extConfiguration.validateAllMapperMethod();
            return sqlSessionFactoryBuilder.build(extConfiguration);
        }
        return result;
    }
}
