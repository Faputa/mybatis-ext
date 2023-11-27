package io.github.mybatisext.spring;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionFactoryBean;

import io.github.mybatisext.ExtConfiguration;

public class ExtSqlSessionFactoryBean extends SqlSessionFactoryBean {

    private final SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();

    protected SqlSessionFactory buildSqlSessionFactory() throws Exception {
        SqlSessionFactory sqlSessionFactory = super.buildSqlSessionFactory();
        Configuration configuration = sqlSessionFactory.getConfiguration();
        if (configuration instanceof ExtConfiguration) {
            return sqlSessionFactory;
        }
        ExtConfiguration extConfiguration = new ExtConfiguration(configuration);
        extConfiguration.validateAllMapperMethod();
        return sqlSessionFactoryBuilder.build(extConfiguration);
    }
}
