package com.example;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import com.mysql.cj.jdbc.MysqlDataSource;

public class DemoMain {
    public static void main(String[] args) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL("jdbc:mysql://localhost:3306/visual?serverTimezone=Asia/Shanghai&characterEncoding=utf8&useSSL=false");
        dataSource.setUser("root");
        dataSource.setPassword("root");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        // configuration.addMapper(DemoMapper.class);
        configuration.addMapper(DemoMapper.class);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        SqlSession session = sqlSessionFactory.openSession();
        DemoMapper demoMapper = session.getMapper(DemoMapper.class);
        System.out.println(demoMapper.countDidaTask());
        System.out.println(demoMapper.countDidaTask2());
    }
}