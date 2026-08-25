package io.github.mybatisext.spring;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.adapter.ExtContextLoader;

public class ExtSqlSessionFactoryBean extends SqlSessionFactoryBean {

    private ExtContext extContext = new ExtContext();

    public void setExtContext(ExtContext extContext) {
        this.extContext = extContext;
    }

    @Override
    protected SqlSessionFactory buildSqlSessionFactory() throws Exception {
        SqlSessionFactory sqlSessionFactory = super.buildSqlSessionFactory();
        new ExtContextLoader(sqlSessionFactory.getConfiguration(), extContext).load();
        return sqlSessionFactory;
    }

}
