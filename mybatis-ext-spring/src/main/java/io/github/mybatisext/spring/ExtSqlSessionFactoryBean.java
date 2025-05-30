package io.github.mybatisext.spring;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionFactoryBean;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ConfigurationInterface;
import io.github.mybatisext.adapter.ExtContext;

public class ExtSqlSessionFactoryBean extends SqlSessionFactoryBean {

    private ExtContext extContext = new ExtContext();

    public void setExtContext(ExtContext extContext) {
        this.extContext = extContext;
    }

    @Override
    protected SqlSessionFactory buildSqlSessionFactory() throws Exception {
        return buildSqlSessionFactory(super.buildSqlSessionFactory());
    }

    public SqlSessionFactory buildSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        Configuration configuration = sqlSessionFactory.getConfiguration();
        if (configuration instanceof ConfigurationInterface) {
            return sqlSessionFactory;
        }
        Configuration extConfiguration = ConfigurationFactory.create(configuration, extContext);
        SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
        return sqlSessionFactoryBuilder.build(extConfiguration);
    }
}
