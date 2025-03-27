package io.github.mybatisext.adapter;

import java.lang.reflect.Constructor;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.exception.MybatisExtException;
import javassist.util.proxy.ProxyFactory;

public class ConfigurationFactory {

    public static Configuration create(ExtContext extContext) {
        return create(new Configuration(), extContext);
    }

    public static Configuration create(Environment environment, ExtContext extContext) {
        return create(new Configuration(environment), extContext);
    }

    public static Configuration create(Configuration configuration, ExtContext extContext) {
        ExtEnhancer extEnhancer = new ExtEnhancer(configuration, extContext);
        Class<?>[] constructorParamTypes = new Class[0];
        Object[] constructorArgs = new Object[0];
        for (Constructor<?> constructor : configuration.getClass().getDeclaredConstructors()) {
            constructorParamTypes = constructor.getParameterTypes();
            constructorArgs = new Object[constructorParamTypes.length];
            constructor.setAccessible(true);
            if (constructorParamTypes.length == 0) {
                break;
            }
        }
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setSuperclass(configuration.getClass());
        proxyFactory.setInterfaces(new Class[]{ConfigurationInterface.class});
        try {
            return (Configuration) proxyFactory.create(constructorParamTypes, constructorArgs, extEnhancer);
        } catch (ReflectiveOperationException e) {
            throw new MybatisExtException(e);
        }
    }
}
