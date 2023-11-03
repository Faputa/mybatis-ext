package com.example;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import com.mysql.cj.jdbc.MysqlDataSource;

public class DemoExtMain {
    public static void main(String[] args) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabaseName("visual");
        dataSource.setUser("root");
        dataSource.setPassword("root");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtConfiguration configuration = new ExtConfiguration(environment);
        // 先注册子类，此时无法确定DemoExtMapper中所有的方法
        // mapper.xml的路径默认和mapper.class一样
        configuration.addMapper(DemoExtMapper.class);
        // 再注册父类
        configuration.addMapper(DemoMapper.class);
        configuration.validateAllMapperMethod(false);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        SqlSession session = sqlSessionFactory.openSession();
        DemoExtMapper demoMapper = session.getMapper(DemoExtMapper.class);
        System.out.println("########" + demoMapper.countDidaTask());
        System.out.println("########" + demoMapper.countDidaTask2());
    }
}