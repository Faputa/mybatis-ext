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

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ConfigurationInterface;
import io.github.mybatisext.adapter.ExtContext;

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
        Configuration configuration = ConfigurationFactory.create(environment, new ExtContext());
        configuration.addMapper(DerivedSysUserMapper.class);
        configuration.addMapper(BaseSysUserMapper.class);
        ((ConfigurationInterface) configuration).validateAllMapperMethod();
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            DerivedSysUserMapper mapper = session.getMapper(DerivedSysUserMapper.class);
            assertEquals(2L, mapper.countAllUsers());
            assertEquals(2L, mapper.countByCreateBy("admin").get());
            assertEquals(2L, session.<Long>selectOne("countAllUsers"));
            assertEquals(2L, session.<Long>selectOne("countByCreateBy", "admin"));
        }
    }
}
