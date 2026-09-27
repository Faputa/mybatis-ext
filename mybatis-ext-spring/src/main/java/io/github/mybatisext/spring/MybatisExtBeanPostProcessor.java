package io.github.mybatisext.spring;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;

import io.github.mybatisext.adapter.ExtContext;

public class MybatisExtBeanPostProcessor implements BeanPostProcessor {

    private ExtContext extContext = new ExtContext();
    // 延迟依赖注入
    private ObjectFactory<ExtContext> extContextFactory = () -> extContext;

    public void setExtContext(ExtContext extContext) {
        this.extContext = extContext;
    }

    public void setExtContextFactory(ObjectFactory<ExtContext> extContextFactory) {
        this.extContextFactory = extContextFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SqlSessionFactory) {
            return buildSqlSessionFactory((SqlSessionFactory) bean);
        }
        if (bean instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) bean;
            if (map.values().stream().anyMatch(v -> v instanceof SqlSessionFactory)) {
                Map<Object, Object> enhanced = new HashMap<>();
                map.forEach((key, value) -> enhanced.put(key, value instanceof SqlSessionFactory ? buildSqlSessionFactory((SqlSessionFactory) value) : value));
                return enhanced;
            }
        }
        return bean;
    }

    private SqlSessionFactory buildSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        ExtSqlSessionFactoryBean extSqlSessionFactoryBean = new ExtSqlSessionFactoryBean();
        extSqlSessionFactoryBean.setExtContext(extContextFactory.getObject());
        return extSqlSessionFactoryBean.buildSqlSessionFactory(sqlSessionFactory);
    }
}
