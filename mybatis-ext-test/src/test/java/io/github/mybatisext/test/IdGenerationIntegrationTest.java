package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

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
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.idgenerator.IdGenerator;
import io.github.mybatisext.mapper.BaseMapper;

class IdGenerationIntegrationTest {

    @Test
    void generatesUuidWhilePreservingPresetValue() throws Exception {
        DataSource dataSource = createDataSource();
        SqlSessionFactory factory = createSqlSessionFactory(dataSource, UuidSysUserMapper.class);

        try (SqlSession session = factory.openSession()) {
            UuidSysUserMapper mapper = session.getMapper(UuidSysUserMapper.class);
            UuidSysUser generated = new UuidSysUser();
            generated.setLoginName("uuid-generated");
            generated.setUserName("generated");
            assertEquals(1, mapper.save(generated));

            UuidSysUser preset = new UuidSysUser();
            preset.setEmail("preset-id");
            preset.setLoginName("uuid-preset");
            preset.setUserName("preset");
            assertEquals(1, mapper.save(preset));
            session.commit();
        }

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT email, user_name FROM sys_user WHERE login_name IN ('uuid-generated', 'uuid-preset') ORDER BY user_name")) {
            resultSet.next();
            String generatedId = resultSet.getString("email");
            assertNotNull(generatedId);
            assertEquals(32, generatedId.length());
            assertEquals("generated", resultSet.getString("user_name"));
            resultSet.next();
            assertEquals("preset-id", resultSet.getString("email"));
            assertEquals("preset", resultSet.getString("user_name"));
        }
    }

    @Test
    void generatesCustomIdWhilePreservingPresetValue() throws Exception {
        DataSource dataSource = createDataSource();
        SqlSessionFactory factory = createSqlSessionFactory(dataSource, CustomSysUserMapper.class);

        try (SqlSession session = factory.openSession()) {
            CustomSysUserMapper mapper = session.getMapper(CustomSysUserMapper.class);
            CustomSysUser generated = new CustomSysUser();
            generated.setLoginName("custom-generated");
            generated.setUserName("generated");
            assertEquals(1, mapper.save(generated));

            CustomSysUser preset = new CustomSysUser();
            preset.setEmail("preset-id");
            preset.setLoginName("custom-preset");
            preset.setUserName("preset");
            assertEquals(1, mapper.save(preset));
            session.commit();
        }

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT email, user_name FROM sys_user WHERE login_name IN ('custom-generated', 'custom-preset') ORDER BY user_name")) {
            resultSet.next();
            assertEquals("custom-generated-id", resultSet.getString("email"));
            assertEquals("generated", resultSet.getString("user_name"));
            resultSet.next();
            assertEquals("preset-id", resultSet.getString("email"));
            assertEquals("preset", resultSet.getString("user_name"));
        }
    }

    @Test
    void delegatesAutoIdGenerationToDatabase() throws Exception {
        DataSource dataSource = createDataSource();
        SqlSessionFactory factory = createSqlSessionFactory(dataSource, AutoSysUserMapper.class);

        try (SqlSession session = factory.openSession()) {
            AutoSysUser user = new AutoSysUser();
            user.setLoginName("auto-id-user");
            user.setUserName("generated");
            assertEquals(1, session.getMapper(AutoSysUserMapper.class).save(user));
            session.commit();
        }

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT user_id, user_name FROM sys_user WHERE login_name = 'auto-id-user'")) {
            resultSet.next();
            assertEquals(100L, resultSet.getLong("user_id"));
            assertEquals("generated", resultSet.getString("user_name"));
        }
    }

    @Test
    void usesCompositeIdsForBaseMapperUpdateAndDelete() throws Exception {
        DataSource dataSource = createDataSource();
        SqlSessionFactory factory = createSqlSessionFactory(dataSource, CompositeSysUserMapper.class);

        try (SqlSession session = factory.openSession()) {
            CompositeSysUserMapper mapper = session.getMapper(CompositeSysUserMapper.class);
            CompositeSysUser admin = new CompositeSysUser(1L, "admin", "1");
            admin.setStatus("1");
            assertEquals(1, mapper.update(admin));
            assertEquals("1", mapper.get(new CompositeSysUser(1L, "admin", null)).getStatus());
            assertEquals("0", mapper.get(new CompositeSysUser(2L, "ry", null)).getStatus());

            assertEquals(1, mapper.delete(new CompositeSysUser(1L, "admin", null)));
            assertNull(mapper.get(new CompositeSysUser(1L, "admin", null)));
            assertNotNull(mapper.get(new CompositeSysUser(2L, "ry", null)));
            session.commit();
        }
    }

    @Test
    void performsBatchCrudWithCompositeIds() throws Exception {
        DataSource dataSource = createDataSource();
        SqlSessionFactory factory = createSqlSessionFactory(dataSource, CompositeSysUserMapper.class);

        try (SqlSession session = factory.openSession()) {
            CompositeSysUserMapper mapper = session.getMapper(CompositeSysUserMapper.class);
            CompositeSysUser first = new CompositeSysUser(98L, "batch-first", "1");
            CompositeSysUser second = new CompositeSysUser(99L, "batch-second", null);
            mapper.saveBatchIgnoreNull(Arrays.asList(first, second));

            assertEquals("1", mapper.get(new CompositeSysUser(98L, "batch-first", null)).getStatus());
            assertEquals("0", mapper.get(new CompositeSysUser(99L, "batch-second", null)).getStatus());

            first.setStatus("0");
            second.setStatus("1");
            mapper.updateBatchIgnoreNull(Arrays.asList(first, second));
            assertEquals("0", mapper.get(new CompositeSysUser(98L, "batch-first", null)).getStatus());
            assertEquals("1", mapper.get(new CompositeSysUser(99L, "batch-second", null)).getStatus());

            mapper.deleteBatch(Arrays.asList(first, second));
            assertNull(mapper.get(new CompositeSysUser(98L, "batch-first", null)));
            assertNull(mapper.get(new CompositeSysUser(99L, "batch-second", null)));
            session.commit();
        }
    }

    private DataSource createDataSource() throws Exception {
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .addScript("data.sql")
                .setScriptEncoding("UTF-8")
                .build();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE sys_user ALTER COLUMN user_id RESTART WITH 100");
        }
        return dataSource;
    }

    private SqlSessionFactory createSqlSessionFactory(DataSource dataSource, Class<?> mapperType) {
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = ConfigurationFactory.create(environment, new ExtContext());
        configuration.addMapper(mapperType);
        ((ConfigurationInterface) configuration).validateAllMapperMethod();
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    @Table(name = "sys_user")
    public static class UuidSysUser {
        @Id(idType = IdType.UUID)
        @Column
        private String email;
        @Column
        private String loginName;
        @Column
        private String userName;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getLoginName() {
            return loginName;
        }

        public void setLoginName(String loginName) {
            this.loginName = loginName;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public interface UuidSysUserMapper extends BaseMapper<UuidSysUser> {
    }

    @Table(name = "sys_user")
    public static class CustomSysUser {
        @Id(idType = IdType.CUSTOM, customIdGenerator = CustomIdGenerator.class)
        @Column
        private String email;
        @Column
        private String loginName;
        @Column
        private String userName;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getLoginName() {
            return loginName;
        }

        public void setLoginName(String loginName) {
            this.loginName = loginName;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public interface CustomSysUserMapper extends BaseMapper<CustomSysUser> {
    }

    @Table(name = "sys_user")
    public static class AutoSysUser {
        @Id(idType = IdType.AUTO)
        @Column
        private Long userId;
        @Column
        private String loginName;
        @Column
        private String userName;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getLoginName() {
            return loginName;
        }

        public void setLoginName(String loginName) {
            this.loginName = loginName;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public interface AutoSysUserMapper extends BaseMapper<AutoSysUser> {
    }

    @Table(name = "sys_user")
    public static class CompositeSysUser {
        @Id
        @Column
        private Long userId;
        @Id
        @Column
        private String loginName;
        @Column
        private String status;

        public CompositeSysUser() {
        }

        public CompositeSysUser(Long userId, String loginName, String status) {
            this.userId = userId;
            this.loginName = loginName;
            this.status = status;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getLoginName() {
            return loginName;
        }

        public void setLoginName(String loginName) {
            this.loginName = loginName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public interface CompositeSysUserMapper extends BaseMapper<CompositeSysUser> {
    }

    public static class CustomIdGenerator implements IdGenerator<String> {
        @Override
        public String getId() {
            return "custom-generated-id";
        }
    }
}
