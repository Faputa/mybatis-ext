package io.github.mybatisext.spring.boot;

import org.apache.ibatis.session.SqlSessionFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.mybatisext.ExtContext;
import io.github.mybatisext.dialect.DialectSelector;
import io.github.mybatisext.spring.ExtSqlSessionFactoryBean;

@Aspect
@Component
public class MybatisExtAspect {

    @Autowired
    private MybatisExtProperties properties;
    @Autowired
    private DialectSelector dialectSelector;

    @Around("@annotation(org.springframework.context.annotation.Bean) && execution(org.apache.ibatis.session.SqlSessionFactory+ *(..))")
    public Object aroundBeanMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (result instanceof SqlSessionFactory) {
            ExtSqlSessionFactoryBean extSqlSessionFactoryBean = new ExtSqlSessionFactoryBean();
            ExtContext extContext = properties.toExtContext();
            extContext.setDialectSelector(dialectSelector);
            extSqlSessionFactoryBean.setExtContext(extContext);
            return extSqlSessionFactoryBean.buildSqlSessionFactory((SqlSessionFactory) result);
        }
        return result;
    }
}
