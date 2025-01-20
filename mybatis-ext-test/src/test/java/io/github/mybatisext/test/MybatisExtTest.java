package io.github.mybatisext.test;

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
        // 先注册子类，此时无法确定SysUserMapper0中所有的方法
        // mapper.xml的路径默认和mapper.class一样
        configuration.addMapper(SysUserMapper1.class);
        // 再注册父类
        configuration.addMapper(SysUserMapper0.class);
        configuration.validateAllMapperMethod();
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        SqlSession session = sqlSessionFactory.openSession();
        SysUserMapper1 sysUserMapper1 = session.getMapper(SysUserMapper1.class);
        System.out.println(sysUserMapper1.countSysUser());
        System.out.println(sysUserMapper1.countByCreateBy("admin"));
        System.out.println(session.<Long>selectOne("countSysUser"));
        System.out.println(session.<Long>selectOne("countByCreateBy", "admin"));
    }
}