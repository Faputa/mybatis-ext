package io.github.mybatisext.integration;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import io.github.mybatisext.adapter.ExtConfiguration;
import io.github.mybatisext.adapter.ExtContext;

public class MybatisExtTest {

    @Test
    public void test() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .addScript("data.sql").setScriptEncoding("UTF-8")
                .build();

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtConfiguration configuration = new ExtConfiguration(environment, new ExtContext());
        // 先注册子类，此时无法确定SysPostMapper0中所有的方法
        // mapper.xml的路径默认和mapper.class一样
        configuration.addMapper(SysPostMapper.class);
        // 再注册父类
        configuration.addMapper(SysPostMapper0.class);
        configuration.validateAllMapperMethod();
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        SqlSession session = sqlSessionFactory.openSession();
        SysPostMapper sysPostMapper = session.getMapper(SysPostMapper.class);
        System.out.println(sysPostMapper.countSysPost());
        System.out.println(sysPostMapper.countByCreateBy("admin"));
        System.out.println(session.<Long>selectOne("countSysPost"));
        System.out.println(session.<Long>selectOne("countByCreateBy", "admin"));
    }
}