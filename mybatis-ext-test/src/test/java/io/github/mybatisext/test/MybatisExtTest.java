package io.github.mybatisext.test;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.ExtConfiguration;
import com.mysql.cj.jdbc.MysqlDataSource;

public class MybatisExtTest {

    @Test
    public void test() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabaseName("visual");
        dataSource.setUser("root");
        dataSource.setPassword("root");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtConfiguration configuration = new ExtConfiguration(environment);
        // 先注册子类，此时无法确定CameraExtMapper中所有的方法
        // mapper.xml的路径默认和mapper.class一样
        configuration.addMapper(CameraMapper.class);
        // 再注册父类
        configuration.addMapper(CameraMapper0.class);
        configuration.validateAllMapperMethod();
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        SqlSession session = sqlSessionFactory.openSession();
        CameraMapper cameraMapper = session.getMapper(CameraMapper.class);
        System.out.println(cameraMapper.countCamera());
        System.out.println(cameraMapper.countCamera2());
    }
}