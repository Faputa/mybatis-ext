package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sql.DataSource;

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

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.adapter.ExtContextLoader;

class EnhancedMapperRegistrationIntegrationTest {

    @Test
    void enhancesMethodsWhenChildMapperIsRegisteredBeforeParentMapper() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .addScript("data.sql").setScriptEncoding("UTF-8")
                .build();

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(DerivedSysUserMapper.class);
        configuration.addMapper(BaseSysUserMapper.class);
        new ExtContextLoader(configuration, new ExtContext()).load();
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            DerivedSysUserMapper mapper = session.getMapper(DerivedSysUserMapper.class);
            assertEquals(2L, mapper.countAllUsers());
            assertEquals(2L, mapper.countByCreateBy("admin").get());
            // countAllUsers是用户XML语句（BaseSysUserMapper.xml），不复制到子命名空间，经Base命名空间访问
            assertEquals(2L, session.<Long>selectOne(BaseSysUserMapper.class.getName() + ".countAllUsers"));
            // countByCreateBy无用户语句，各命名空间各自生成
            assertEquals(2L, session.<Long>selectOne(DerivedSysUserMapper.class.getName() + ".countByCreateBy", "admin"));
        }
    }
}
