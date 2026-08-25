package io.github.mybatisext.spring;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.adapter.ExtContextLoader;

public class MybatisExtBeanPostProcessor implements BeanPostProcessor, SmartInitializingSingleton {

    private ExtContext extContext = new ExtContext();
    // 延迟依赖注入
    private ObjectFactory<ExtContext> extContextFactory = () -> extContext;
    private Map<String, SqlSessionFactory> sqlSessionFactories = Collections.emptyMap();

    public void setExtContext(ExtContext extContext) {
        this.extContext = extContext;
    }

    public void setExtContextFactory(ObjectFactory<ExtContext> extContextFactory) {
        this.extContextFactory = extContextFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SqlSessionFactory) {
            track(beanName, (SqlSessionFactory) bean);
            doLoad((SqlSessionFactory) bean);
        } else if (bean instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) bean;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() instanceof SqlSessionFactory) {
                    track(String.valueOf(entry.getKey()), (SqlSessionFactory) entry.getValue());
                    doLoad((SqlSessionFactory) entry.getValue());
                }
            }
        } else if (bean instanceof MapperFactoryBean) {
            enhance((MapperFactoryBean<?>) bean);
        }
        return bean;
    }

    // 兜底：覆盖绕过spring-bean体系创建的工厂等场景，load幂等可重复调用
    @Override
    public void afterSingletonsInstantiated() {
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactories.values()) {
            doLoad(sqlSessionFactory);
        }
    }

    // checkDaoConfig已完成addMapper，此处对刚注册的mapper做增量增强
    private void enhance(MapperFactoryBean<?> mapperFactoryBean) {
        Class<?> mapperInterface = mapperFactoryBean.getMapperInterface();
        if (mapperInterface == null) {
            return;
        }
        ExtContext extContext = extContextFactory.getObject();
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactories.values()) {
            if (sqlSessionFactory.getConfiguration().hasMapper(mapperInterface)) {
                new ExtContextLoader(sqlSessionFactory.getConfiguration(), extContext).load(mapperInterface);
            }
        }
    }

    private void track(String key, SqlSessionFactory sqlSessionFactory) {
        if (sqlSessionFactories.containsKey(key) && sqlSessionFactories.get(key) == sqlSessionFactory) {
            return;
        }
        Map<String, SqlSessionFactory> tracked = new LinkedHashMap<>(sqlSessionFactories);
        tracked.put(key, sqlSessionFactory);
        sqlSessionFactories = tracked;
    }

    private void doLoad(SqlSessionFactory sqlSessionFactory) {
        ExtContext extContext = extContextFactory.getObject();
        new ExtContextLoader(sqlSessionFactory.getConfiguration(), extContext).load();
    }
}
