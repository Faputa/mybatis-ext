package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class IncrementalMapperLoadingIntegrationTest {

    @Test
    void loadsSingleMapperGeneratingAllMethodsUnderItsOwnNamespace() {
        Configuration configuration = createConfiguration();
        configuration.addMapper(DerivedSysUserMapper.class);
        configuration.addMapper(BaseSysUserMapper.class);
        ExtContextLoader loader = new ExtContextLoader(configuration, new ExtContext());

        loader.load(DerivedSysUserMapper.class);

        String derivedCount = DerivedSysUserMapper.class.getName() + ".countByCreateBy";
        String baseCount = BaseSysUserMapper.class.getName() + ".countByCreateBy";
        assertTrue(configuration.hasStatement(derivedCount));
        assertFalse(configuration.hasStatement(baseCount));
        // 用户自定义语句（BaseSysUserMapper.xml）不复制到子命名空间
        assertFalse(configuration.hasStatement(DerivedSysUserMapper.class.getName() + ".executeTwoDeleteStatements"));
        assertTrue(configuration.hasStatement(BaseSysUserMapper.class.getName() + ".executeTwoDeleteStatements"));

        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            assertEquals(2L, session.<Long>selectOne(derivedCount, "admin"));
            DerivedSysUserMapper mapper = session.getMapper(DerivedSysUserMapper.class);
            assertEquals(2L, mapper.countAllUsers());
            assertEquals(2L, mapper.countByCreateBy("admin").get());
        }

        loader.load(BaseSysUserMapper.class);
        assertTrue(configuration.hasStatement(baseCount));
        try (SqlSession session = sqlSessionFactory.openSession()) {
            BaseSysUserMapper mapper = session.getMapper(BaseSysUserMapper.class);
            assertEquals(2L, mapper.countAllUsers());
            assertEquals(2L, mapper.countByCreateBy("admin").get());
        }

        int statementCount = configuration.getMappedStatementNames().size();
        loader.load(DerivedSysUserMapper.class);
        loader.load(BaseSysUserMapper.class);
        loader.load();
        assertEquals(statementCount, configuration.getMappedStatementNames().size());
    }

    private Configuration createConfiguration() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .addScript("data.sql").setScriptEncoding("UTF-8")
                .build();
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        return new Configuration(environment);
    }
}
