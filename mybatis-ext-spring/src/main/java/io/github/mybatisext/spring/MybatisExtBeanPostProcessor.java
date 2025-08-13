package io.github.mybatisext.spring;

import java.util.Map;
import java.util.stream.Collectors;

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
                return map.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> buildSqlSessionFactory((SqlSessionFactory) e.getValue())));
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
