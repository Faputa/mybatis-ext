package io.github.mybatisext.spring.boot;

import org.apache.ibatis.session.SqlSessionFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.mybatisext.spring.ExtSqlSessionFactoryBean;

@Aspect
@Component
public class MybatisExtAspect {

    @Autowired
    private MybatisExtProperties properties;

    @Around("@annotation(org.springframework.context.annotation.Bean)")
    public Object aroundBeanMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (result instanceof SqlSessionFactory) {
            ExtSqlSessionFactoryBean extSqlSessionFactoryBean = new ExtSqlSessionFactoryBean();
            extSqlSessionFactoryBean.setExtContext(properties.toExtContext());
            return extSqlSessionFactoryBean.buildSqlSessionFactory((SqlSessionFactory) result);
        }
        return result;
    }
}
