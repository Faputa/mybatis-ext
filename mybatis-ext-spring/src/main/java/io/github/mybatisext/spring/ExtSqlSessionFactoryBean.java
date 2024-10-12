package io.github.mybatisext.spring;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionFactoryBean;

import io.github.mybatisext.ExtConfiguration;
import io.github.mybatisext.ExtContext;

public class ExtSqlSessionFactoryBean extends SqlSessionFactoryBean {

    private ExtContext extContext = new ExtContext();

    private final SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();

    public void setExtContext(ExtContext extContext) {
        this.extContext = extContext;
    }

    @Override
    protected SqlSessionFactory buildSqlSessionFactory() throws Exception {
        return buildSqlSessionFactory(super.buildSqlSessionFactory());
    }

    public SqlSessionFactory buildSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        Configuration configuration = sqlSessionFactory.getConfiguration();
        if (configuration instanceof ExtConfiguration) {
            return sqlSessionFactory;
        }
        ExtConfiguration extConfiguration = new ExtConfiguration(configuration, extContext);
        extConfiguration.validateAllMapperMethod();
        return sqlSessionFactoryBuilder.build(extConfiguration);
    }
}
