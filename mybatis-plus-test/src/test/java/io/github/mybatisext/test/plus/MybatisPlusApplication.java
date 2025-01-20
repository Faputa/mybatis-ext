package io.github.mybatisext.test.plus;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@SpringBootApplication
public class MybatisPlusApplication implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb_1")
                .addScript("schema.sql")
                .addScript("data.sql").setScriptEncoding("UTF-8")
                .build();
        new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb_2")
                .addScript("schema.sql")
                .addScript("data.sql").setScriptEncoding("UTF-8")
                .build();
    }
}
