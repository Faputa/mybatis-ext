package io.github.mybatisext.spring.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("io.github.mybatisext.test")
@SpringBootApplication
public class MybatisExtSpringApplication {
}
