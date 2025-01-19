package io.github.mybatisext.spring;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import io.github.mybatisext.adapter.ExtContext;

public class MybatisExtBeanPostProcessor implements BeanPostProcessor {

    private ExtContext extContext = new ExtContext();

    public void setExtContext(ExtContext extContext) {
        this.extContext = extContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SqlSessionFactory) {
            ExtSqlSessionFactoryBean extSqlSessionFactoryBean = new ExtSqlSessionFactoryBean();
            extSqlSessionFactoryBean.setExtContext(extContext);
            return extSqlSessionFactoryBean.buildSqlSessionFactory((SqlSessionFactory) bean);
        }
        return bean;
    }
}
