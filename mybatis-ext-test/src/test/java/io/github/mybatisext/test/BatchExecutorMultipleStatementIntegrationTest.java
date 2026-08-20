package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sql.DataSource;

import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

class BatchExecutorMultipleStatementIntegrationTest {

    @Test
    void returnsBatchExecutorSentinelForMultipleSqlStatements() {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .addScript("data.sql").setScriptEncoding("UTF-8")
                .build();

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(BaseSysUserMapper.class);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            BaseSysUserMapper mapper = session.getMapper(BaseSysUserMapper.class);
            assertEquals(BatchExecutor.BATCH_UPDATE_RETURN_VALUE, mapper.executeTwoDeleteStatements());
        }
    }
}
