package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.Driver;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.adapter.ExtContextLoader;
import io.github.mybatisext.dialect.Dialect;

abstract class AbstractDialectCrudIntegrationTest {

    @Test
    void queriesByMethodNameObjectAndRelations() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);

            SysUser admin = mapper.getByLoginName("admin");
            assertNotNull(admin);
            assertEquals(1L, admin.getUserId());
            assertEquals(101L, admin.getParentId());
            assertEquals("研发部门", admin.getDept().getDeptName());
            assertEquals(1L, admin.getRoleIds().get(0));
            assertEquals(1L, admin.getPostIds().get(0));

            assertEquals(2, mapper.countByStatus("0"));
            assertTrue(mapper.existsByEmail("ry@qq.com"));
            assertFalse(mapper.existsByEmail("missing@example.com"));

            SysUser query = new SysUser();
            query.setUserId(2L);
            assertEquals("ry", mapper.getByUserId(query).getLoginName());
            assertEquals("ry", mapper.getByUserId(2L).getLoginName());
        }
    }

    @Test
    void supportsDtoProjectionInheritanceAndQueryOperators() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);

            SysUserVO voQuery = new SysUserVO();
            voQuery.setLoginName("admin");
            List<SysUser> users = mapper.list(voQuery);
            assertEquals(1, users.size());
            assertEquals(1L, users.get(0).getUserId());

            SysUser inheritedQuery = new SysUser();
            inheritedQuery.setCreateBy("admin");
            assertEquals(2, mapper.count(inheritedQuery));

            SysUser projectionQuery = new SysUser();
            projectionQuery.setUserId(2L);
            List<SysUserVO> projections = mapper.listSysUserVO(projectionQuery);
            assertEquals(1, projections.size());
            assertEquals("ry", projections.get(0).getLoginName());
            assertEquals(2L, projections.get(0).getRoles2().get(0).getRoleId());

            List<SysUser> top = mapper.listTop1ByStatusOrderByUserIdDesc("0");
            assertEquals(1, top.size());
            assertEquals(2L, top.get(0).getUserId());

            List<SysUser> in = mapper.listByUserIdInOrderByUserIdDesc(Arrays.asList(1L, 2L));
            assertEquals(Arrays.asList(2L, 1L), Arrays.asList(in.get(0).getUserId(), in.get(1).getUserId()));
        }
    }

    @Test
    void queriesWithRangeLogicalLikeAndNullOperators() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);

            assertEquals(Arrays.asList(1L, 2L), userIds(mapper.listByUserIdBetweenStartToEndOrderByUserId(1L, 2L)));
            assertEquals(Arrays.asList(2L), userIds(mapper.listByLoginNameOrUserIdGreaterThanOrderByUserId("missing", 1L)));
            assertEquals(Arrays.asList(1L), userIds(mapper.listByLoginNameStartWithOrderByUserId("ad")));
            assertEquals(Arrays.asList(2L), userIds(mapper.listByLoginNameEndWithOrderByUserId("y")));
            assertEquals(Arrays.asList(1L, 2L), userIds(mapper.listByLoginDateIsNullAndPwdUpdateDateIsNullOrderByUserId()));
        }
    }

    @Test
    void savesUsersInBatchWithPerRowNullSemantics() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            SysUser first = createUser(98L, "batch-first");
            first.setStatus("1");
            SysUser second = createUser(99L, "batch-second");
            second.setStatus(null);

            assertTrue(mapper.saveBatchIgnoreNull(Arrays.asList(first, second)) > 0);
            assertEquals("1", mapper.get(idQuery(98L)).getStatus());
            assertEquals("0", mapper.get(idQuery(99L)).getStatus());
        }
    }

    @Test
    void updatesAndDeletesUsersInBatch() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            SysUser first = new SysUser();
            first.setUserId(1L);
            first.setUserName("batch-updated");
            SysUser second = new SysUser();
            second.setUserId(2L);
            second.setStatus("1");

            assertTrue(mapper.updateBatchIgnoreNull(Arrays.asList(first, second)) > 0);
            assertEquals("batch-updated", mapper.get(idQuery(1L)).getUserName());
            assertEquals("0", mapper.get(idQuery(1L)).getStatus());
            assertEquals("若依", mapper.get(idQuery(2L)).getUserName());
            assertEquals("1", mapper.get(idQuery(2L)).getStatus());

            assertTrue(mapper.deleteBatch(Arrays.asList(first, second)) > 0);
            assertNull(mapper.get(idQuery(1L)));
            assertNull(mapper.get(idQuery(2L)));
        }
    }

    @Test
    void appliesSaveAndUpdateNullSemantics() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);

            SysUser user = new SysUser();
            user.setUserId(98L);
            user.setLoginName("default-value-user");
            assertEquals(1, mapper.saveIgnoreNull(user));

            SysUser idQuery = new SysUser();
            idQuery.setUserId(98L);
            assertEquals("0", mapper.get(idQuery).getStatus());

            SysUser admin = mapper.getByLoginName("admin");
            String originalEmail = admin.getEmail();
            admin.setEmail(null);
            admin.setUserName("ignore-null-update");
            assertEquals(1, mapper.updateIgnoreNull(admin));
            assertEquals(originalEmail, mapper.getByLoginName("admin").getEmail());

            admin = mapper.getByLoginName("admin");
            admin.setEmail(null);
            assertEquals(1, mapper.update(admin));
            assertNull(mapper.getByLoginName("admin").getEmail());
        }
    }

    @Test
    void savesAndGetsUser() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            SysUser user = createUser();

            assertEquals(1, mapper.saveIgnoreNull(user));

            SysUser savedUser = mapper.get(idQuery(99L));
            assertNotNull(savedUser);
            assertEquals("dialect-user", savedUser.getLoginName());
            assertEquals("方言测试用户", savedUser.getUserName());
        }
    }

    @Test
    void updatesUser() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            SysUser user = new SysUser();
            user.setUserId(2L);
            user.setUserName("修改后");

            assertEquals(1, mapper.updateIgnoreNull(user));
            assertEquals("修改后", mapper.get(idQuery(2L)).getUserName());
        }
    }

    @Test
    void listsUsersWithDialectPagination() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            List<SysUser> users = session.getMapper(SysUserMapper.class).listByStatusOrderByUserIdLimitOffsetToRowCount("0", 0, 2);

            assertEquals(2, users.size());
            assertEquals("admin", users.get(0).getLoginName());
            assertEquals("ry", users.get(1).getLoginName());
        }
    }

    @Test
    void listsUsersWithFixedDialectPagination() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            List<SysUser> users = session.getMapper(SysUserMapper.class).listByStatusOrderByUserIdLimit1To1("0");

            assertEquals(1, users.size());
            assertEquals("ry", users.get(0).getLoginName());
        }
    }

    @Test
    void countsUsers() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            SysUser query = new SysUser();
            query.setStatus("0");

            assertEquals(2, session.getMapper(SysUserMapper.class).count(query));
        }
    }

    @Test
    void checksUserExistence() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);

            assertTrue(mapper.exists(idQuery(1L)));
            assertFalse(mapper.exists(idQuery(99L)));
        }
    }

    @Test
    void deletesUser() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);

            assertEquals(1, mapper.delete(idQuery(2L)));
            assertNull(mapper.get(idQuery(2L)));
        }
    }

    protected abstract DialectDataSource createDataSource() throws Exception;

    protected DialectDataSource newDataSource(Dialect dialect, String compatibilityMode) throws Exception {
        String databaseName = dialect.getClass().getSimpleName() + "_" + System.nanoTime();
        String url = "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1";
        Driver driver = new Driver();
        SimpleDriverDataSource initializationDataSource = new SimpleDriverDataSource(driver, url, "sa", "");
        try (Connection connection = initializationDataSource.getConnection()) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"), new ClassPathResource("data.sql"));
            populator.setSqlScriptEncoding("UTF-8");
            populator.populate(connection);
        }
        if (!compatibilityMode.isEmpty()) {
            url += ";MODE=" + compatibilityMode;
        }
        return new DialectDataSource(driver, url, dialect);
    }

    protected SqlSessionFactory createSqlSessionFactory() throws Exception {
        return createSqlSessionFactory(createDataSource());
    }

    private SqlSessionFactory createSqlSessionFactory(DialectDataSource dataSource) {
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtContext extContext = new ExtContext();
        extContext.setDialectSelector(jdbcUrl -> dataSource.getDialect());
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(SysUserMapper.class);
        new ExtContextLoader(configuration, extContext).load();
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private SysUser createUser() {
        return createUser(99L, "dialect-user");
    }

    private SysUser createUser(Long userId, String loginName) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setLoginName(loginName);
        user.setUserName("方言测试用户");
        user.setStatus("0");
        user.setCreateBy("tester");
        return user;
    }

    private List<Long> userIds(List<SysUser> users) {
        return users.stream().map(SysUser::getUserId).collect(java.util.stream.Collectors.toList());
    }

    private SysUser idQuery(Long userId) {
        SysUser query = new SysUser();
        query.setUserId(userId);
        return query;
    }

    protected static class DialectDataSource extends SimpleDriverDataSource {
        private final Dialect dialect;

        DialectDataSource(Driver driver, String url, Dialect dialect) {
            super(driver, url, "sa", "");
            this.dialect = dialect;
        }

        Dialect getDialect() {
            return dialect;
        }

    }
}
