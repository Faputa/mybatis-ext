package io.github.mybatisext.spring.boot;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.dialect.DialectSelector;
import io.github.mybatisext.spring.ExtSqlSessionFactoryBean;

@Component
public class MybatisExtBeanPostProcessor implements BeanPostProcessor {

    @Autowired
    private MybatisExtProperties properties;
    @Autowired
    private DialectSelector dialectSelector;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SqlSessionFactory) {
            ExtSqlSessionFactoryBean extSqlSessionFactoryBean = new ExtSqlSessionFactoryBean();
            ExtContext extContext = properties.toExtContext();
            extContext.setDialectSelector(dialectSelector);
            extSqlSessionFactoryBean.setExtContext(extContext);
            return extSqlSessionFactoryBean.buildSqlSessionFactory((SqlSessionFactory) bean);
        }
        return bean;
    }
}
