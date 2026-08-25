package io.github.mybatisext.spring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<String, ExtContextLoader> loaders = new ConcurrentHashMap<>();

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
        } else if (bean instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) bean;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() instanceof SqlSessionFactory) {
                    track(String.valueOf(entry.getKey()), (SqlSessionFactory) entry.getValue());
                }
            }
        } else if (bean instanceof MapperFactoryBean) {
            enhance((MapperFactoryBean<?>) bean);
        }
        return bean;
    }

    // 兜底：所有单例就绪后全量增强，覆盖XML <mappers>等在工厂构建期注册的场景；load幂等
    @Override
    public void afterSingletonsInstantiated() {
        for (ExtContextLoader loader : loaders.values()) {
            loader.load();
        }
    }

    // checkDaoConfig已完成addMapper，此处对刚注册的mapper增量增强，覆盖懒加载/prototype等启动后创建的场景
    private void enhance(MapperFactoryBean<?> mapperFactoryBean) {
        Class<?> mapperInterface = mapperFactoryBean.getMapperInterface();
        if (mapperInterface == null) {
            return;
        }
        for (ExtContextLoader loader : loaders.values()) {
            if (loader.getConfiguration().hasMapper(mapperInterface)) {
                loader.load(mapperInterface);
            }
        }
    }

    private void track(String key, SqlSessionFactory sqlSessionFactory) {
        loaders.computeIfAbsent(key, k -> new ExtContextLoader(sqlSessionFactory.getConfiguration(), extContextFactory.getObject()));
    }
}
