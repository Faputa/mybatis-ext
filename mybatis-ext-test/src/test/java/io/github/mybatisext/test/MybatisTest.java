package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.sql.DataSource;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

public class MybatisTest {

    @Test
    public void test() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .addScript("data.sql").setScriptEncoding("UTF-8")
                .build();

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(SysUserMapper0.class);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        SqlSession session = sqlSessionFactory.openSession();
        SysUserMapper0 sysUserMapper0 = session.getMapper(SysUserMapper0.class);
        System.out.println(sysUserMapper0.countSysUser());
        assertThrows(BindingException.class, () -> sysUserMapper0.countByCreateBy("admin"));
    }
}