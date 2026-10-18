package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ConfigurationInterface;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.ParserException;
import io.github.mybatisext.mapper.ExtMapper;

class ParserSyntaxIntegrationTest {

    private EmbeddedDatabase database;
    private SqlSession session;
    private SysUserMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .setScriptEncoding("UTF-8")
                .addScript("schema.sql")
                .addScript("data.sql")
                .build();
        Configuration configuration = newConfiguration();
        configuration.addMapper(SysUserMapper.class);
        configuration.addMapper(FlagMapper.class);
        ((ConfigurationInterface) configuration).validateAllMapperMethod();
        session = new SqlSessionFactoryBuilder().build(configuration).openSession();
        mapper = session.getMapper(SysUserMapper.class);
        try (Statement statement = session.getConnection().createStatement()) {
            // 同时保留重复分组值、不同状态、空值和关联部门，避免所有分支都落在同一组数据上。
            statement.execute("INSERT INTO sys_user (user_id, dept_id, login_name, status, email, login_date) VALUES "
                    + "(3, 103, 'Alpha', '1', NULL, NULL), "
                    + "(4, 105, 'beta', '1', 'beta@example.com', TIMESTAMP '2026-01-01 00:00:00'), "
                    + "(5, 103, 'al_ice', '0', 'alice@example.com', NULL)");
            statement.execute("CREATE TABLE syntax_flag (id BIGINT PRIMARY KEY, enabled BOOLEAN)");
            statement.execute("INSERT INTO syntax_flag VALUES (1, TRUE), (2, FALSE), (3, NULL)");
        }
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
        if (database != null) {
            database.shutdown();
        }
    }

    @Test
    void supportsSelectAliasesAllAndWhereAliases() {
        assertIds(mapper.findAllOrderByUserId(), 1L, 2L, 3L, 4L, 5L);
        assertIds(mapper.selectAllOrderByUserId(), 1L, 2L, 3L, 4L, 5L);
        assertEquals(5L, mapper.getOneByStatusOrderByUserIdDesc("0").getUserId());
        assertNull(mapper.getOneByStatusOrderByUserIdDesc("missing"));
        assertIds(mapper.findWhereLoginNameEquals("admin"), 1L);
        assertIds(mapper.selectByLoginNameIs("Alpha"), 3L);
        assertIds(mapper.listByLoginNameIsValue$AndStatusIsState$OrderByUserId("Alpha", "1"), 3L);
        assertIds(mapper.listByLoginNameIsValue$AndStatusIsState$OrderByUserId("Alpha", "0"));
    }

    @Test
    void projectsDistinctScalarsExplicitFieldsAndReturnTypes() {
        assertEquals(Arrays.asList("0", "1"), mapper.listDistinctStatusOrderByStatus());
        List<SysUser> projected = mapper.listUserIdAndLoginNameOrderByUserId();
        assertIds(projected, 1L, 2L, 3L, 4L, 5L);
        assertEquals("admin", projected.get(0).getLoginName());
        assertNull(projected.get(0).getStatus());
        assertNull(projected.get(0).getDeptId());

        SysUser criteria = user(3L, null);
        List<SysUserVO> views = mapper.listSysUserVO(criteria);
        assertEquals(1, views.size());
        assertEquals("Alpha", views.get(0).getLoginName());
    }

    @Test
    void ordersByMultipleFieldsWithIndependentDirections() {
        assertIds(mapper.listOrderByStatusAscAndUserIdDesc(), 5L, 2L, 1L, 4L, 3L);
    }

    @Test
    void comparesNumbersAndNegatesConditions() {
        assertIds(mapper.listByUserIdLessThanOrderByUserId(3), 1L, 2L);
        assertIds(mapper.listByUserIdLessThanEqualOrderByUserId(3), 1L, 2L, 3L);
        assertIds(mapper.listByUserIdGreaterThanEqualOrderByUserId(3), 3L, 4L, 5L);
        assertIds(mapper.listByUserIdNotGreaterThanOrderByUserId(3), 1L, 2L, 3L);
        assertIds(mapper.listByLoginNameNotOrderByUserId("Alpha"), 1L, 2L, 4L, 5L);
        assertIds(mapper.listByUserIdBetweenOrderByUserId(2, 4), 2L, 3L, 4L);
        assertIds(mapper.listByUserIdNotBetweenOrderByUserId(2, 4), 1L, 5L);
        assertIds(mapper.listByUserIdBetweenStartToEndOrderByUserId(2L, 4L), 2L, 3L, 4L);
    }

    @Test
    void transformsStringParametersForLikeAndIgnorecase() {
        assertIds(mapper.listByLoginNameIgnorecaseEqualsOrderByUserId("ALPHA"), 3L);
        assertIds(mapper.listByLoginNameLikeOrderByUserId("dm"), 1L);
        assertIds(mapper.listByLoginNameNotLikeOrderByUserId("a"), 2L);
        assertIds(mapper.listByLoginNameIgnorecaseLikeOrderByUserId("AL"), 3L, 5L);
        assertIds(mapper.listByLoginNameIgnorecaseStartWithOrderByUserId("AL"), 3L, 5L);
        assertIds(mapper.listByLoginNameIgnorecaseEndWithOrderByUserId("HA"), 3L);
        assertIds(mapper.listByLoginNameIgnorecaseNotLikeOrderByUserId("AL"), 1L, 2L, 4L);
        assertIds(mapper.listByLoginNameStartWithOrderByUserId("ad"), 1L);
        assertIds(mapper.listByLoginNameEndWithOrderByUserId("ta"), 4L);
    }

    @Test
    void expandsInCollectionsAndTransformsEachElement() {
        assertIds(mapper.listByUserIdInOrderByUserIdDesc(Arrays.asList(1L, 3L, 5L)), 5L, 3L, 1L);
        assertIds(mapper.listByUserIdNotInOrderByUserId(Arrays.asList(1L, 3L, 5L)), 2L, 4L);
        assertIds(mapper.listByLoginNameIgnorecaseInOrderByUserId(Arrays.asList("ADMIN", "ALPHA")), 1L, 3L);
    }

    @Test
    void evaluatesNullChecksAndTheirNegations() {
        assertIds(mapper.listByEmailIsNullOrderByUserId(), 3L);
        assertIds(mapper.listByEmailIsNotNullOrderByUserId(), 1L, 2L, 4L, 5L);
        assertIds(mapper.listByEmailNotIsNullOrderByUserId(), 1L, 2L, 4L, 5L);
        assertIds(mapper.listByEmailNotIsNotNullOrderByUserId(), 3L);
        assertIds(mapper.listByLoginDateIsNullAndPwdUpdateDateIsNullOrderByUserId(), 1L, 2L, 3L, 5L);
    }

    @Test
    void preservesAndPrecedenceRepeatedConditionsAndBetweenParameterOrder() {
        assertIds(mapper.listByUserIdOrUserIdAndStatusOrderByUserId(1, 3, "0"), 1L);
        assertIds(mapper.listByUserIdAndStatusOrUserIdOrderByUserId(1, "1", 3), 3L);
        assertIds(mapper.listByUserIdOrUserIdOrUserIdOrderByUserId(1, 3, 5), 1L, 3L, 5L);
        assertIds(mapper.listByUserIdIsChosenOrUserIdIsChosenOrderByUserId(3), 3L);
        assertIds(mapper.listByUserIdBetweenOrLoginNameOrderByUserId(2, 3, "al_ice"), 2L, 3L, 5L);
        assertIds(mapper.listByUserIdOrLoginNameIsPatternOrderByUserId("Alpha", 1), 1L, 3L);
    }

    @Test
    void resolvesNestedPropertiesVariablesAndRelationColumns() {
        SysUser criteria = user(3L, null);
        criteria.setDeptId(103L);
        assertIds(mapper.listByUserIdIsCriteriaDotUserIdOrderByUserId(criteria), 3L);
        assertIds(mapper.listByDeptDotDeptIdIsCriteriaDotDeptIdOrderByUserId(criteria), 1L, 3L, 5L);
        assertIds(mapper.listByDeptDotDeptNameOrderByUserId("研发部门"), 1L, 3L, 5L);
    }

    @Test
    void reservesTopAndLimitParametersAndSupportsAllLimitForms() {
        assertIds(mapper.listTopCountByUserIdGreaterThanOrderByUserId(2, 1), 2L, 3L);
        assertIds(mapper.listTop1ByStatusOrderByUserIdDesc("0"), 5L);
        assertIds(mapper.listOrderByUserIdLimit2(), 1L, 2L);
        assertIds(mapper.listOrderByUserIdLimitCount(2), 1L, 2L);
        assertIds(mapper.listOrderByUserIdLimit1To2(), 2L, 3L);
        assertIds(mapper.listOrderByUserIdLimitOffsetToCount(2, 2), 3L, 4L);
        assertIds(mapper.listOrderByUserIdLimit1ToCount(2), 2L, 3L);
        assertIds(mapper.listOrderByUserIdLimitOffsetTo1(2), 3L);
        assertIds(mapper.listTop1OrderByUserIdLimit2(), 1L, 2L);
        assertIds(mapper.listOneOrderByUserIdLimitCount(2), 1L, 2L);
        assertIds(mapper.listByStatusOrderByUserIdLimitOffsetToRowCount("0", 1, 1), 2L);
    }

    @Test
    void groupsWithExplicitAndDefaultHavingAndReservesCrossClauseParameters() {
        assertEquals(Arrays.asList("0", "1"), mapper.listStatusGroupByStatusOrderByStatus());
        assertEquals(Collections.singletonList("1"), mapper.findStatusGroupByStatusOrderByStatus("1"));
        assertEquals(Collections.emptyList(), mapper.findStatusGroupByStatusOrderByStatus(null));
        assertEquals(Collections.singletonList("1"),
                mapper.listStatusByUserIdGreaterThanGroupByStatusHavingStatusIsTargetOrderByStatusLimitCount("1", 1, 2));
        assertEquals(Collections.emptyList(),
                mapper.listStatusByUserIdGreaterThanGroupByStatusHavingStatusIsTargetOrderByStatusLimitCount("missing", 1, 2));
        assertEquals(Arrays.asList(2L, 3L), mapper.listUserIdByUserIdGreaterThanGroupByUserIdHavingUserIdLessThanOrderByUserId(1, 4));
        assertEquals(Collections.singletonList(4L),
                mapper.listUserIdByUserIdOrUserIdGroupByUserIdHavingUserIdGreaterThanThresholdOrderByUserIdLimitCount(2, 1, 1, 4));
        assertEquals(Collections.singletonList(3L), mapper.listUserIdByUserIdGroupByUserId(user(3L, null)));
    }

    @Test
    void groupsByMultipleFieldsAndAppliesHavingLogicalPrecedence() {
        List<SysUserStatusVO> groups = mapper.listDeptIdAndStatusGroupByDeptIdAndStatusOrderByDeptIdAscAndStatusDesc();
        assertEquals(Arrays.asList(103L, 103L, 105L, 105L), groups.stream().map(SysUserStatusVO::getDeptId).collect(Collectors.toList()));
        assertEquals(Arrays.asList("1", "0", "1", "0"), groups.stream().map(SysUserStatusVO::getStatus).collect(Collectors.toList()));
        assertEquals(Arrays.asList(1L, 4L), mapper.listUserIdGroupByUserIdHavingUserIdLessThanOrUserIdGreaterThanAndUserIdLessThanOrderByUserId(2, 3, 5));
    }

    @Test
    void buildsDefaultEntityAndNamedConditionsAndSkipsRowBounds() {
        SysUser criteria = new SysUser();
        criteria.setStatus("1");
        assertEquals(2, mapper.list(criteria).size());
        assertEquals(1, mapper.list(criteria, new RowBounds(1, 1)).size());
        assertIds(mapper.listByUserIdGreaterThanOrderByUserId(new RowBounds(1, 2), 1), 3L, 4L);
        assertEquals(2, mapper.count(criteria));
        assertTrue(mapper.exists(criteria));
        assertEquals(3, mapper.count("0"));
        assertEquals(2, mapper.countWhereUserIdGreaterThan(3));
        assertTrue(mapper.existsWhereUserIdGreaterThan(4));
        assertFalse(mapper.existsWhereUserIdGreaterThan(5));
        assertFalse(mapper.existsByEmail("missing@example.com"));
    }

    @Test
    void insertsWithAliasesBatchAndIgnoreNullSemantics() {
        SysUser explicitNull = user(90L, "insert-null");
        assertEquals(1, mapper.insert(explicitNull));
        assertNull(mapper.getByUserId(90L).getStatus());

        assertEquals(1, mapper.insertIgnoreNull(user(91L, "insert-default")));
        assertEquals("0", mapper.getByUserId(91L).getStatus());
        assertTrue(mapper.insertBatchIgnoreNull(Arrays.asList(user(92L, "batch-first"), user(93L, "batch-second"))) > 0);
        assertEquals("batch-first", mapper.getByUserId(92L).getLoginName());
        assertEquals("0", mapper.getByUserId(93L).getStatus());
    }

    @Test
    void modifiesExplicitFieldsWithAndWithoutIgnoreNull() {
        SysUser update = user(1L, "changed");
        assertEquals(1, mapper.modifyLoginNameAndEmailIgnoreNullWhereUserId(update));
        assertEquals("changed", mapper.getByUserId(1L).getLoginName());
        assertEquals("ry@163.com", mapper.getByUserId(1L).getEmail());
        assertEquals("0", mapper.getByUserId(1L).getStatus());

        update.setLoginName("changed-again");
        assertEquals(1, mapper.modifyLoginNameAndEmailByUserId(update));
        assertEquals("changed-again", mapper.getByUserId(1L).getLoginName());
        assertNull(mapper.getByUserId(1L).getEmail());
        assertEquals("0", mapper.getByUserId(1L).getStatus());
        assertEquals("ry", mapper.getByUserId(2L).getLoginName());
    }

    @Test
    void modifiesBatchFieldsAndRemovesByExplicitAndDefaultConditions() {
        SysUser first = user(1L, null);
        first.setStatus("1");
        SysUser second = user(2L, null);
        second.setStatus("1");
        assertTrue(mapper.modifyBatchStatusIgnoreNullByUserId(Arrays.asList(first, second)) > 0);
        assertEquals("1", mapper.getByUserId(1L).getStatus());
        assertEquals("1", mapper.getByUserId(2L).getStatus());
        assertEquals("admin", mapper.getByUserId(1L).getLoginName());
        assertEquals("0", mapper.getByUserId(5L).getStatus());
        first.setStatus("0");
        second.setStatus("0");
        assertTrue(mapper.updateBatchStatusIgnoreNullByUserId(Arrays.asList(first, second)) > 0);
        assertEquals("0", mapper.getByUserId(1L).getStatus());
        assertEquals("0", mapper.getByUserId(2L).getStatus());

        first.setStatus("1");
        second.setStatus("1");
        assertTrue(mapper.modifyBatchStatusIgnoreNullWhereUserId(new SysUser[]{first, second}) > 0);
        assertEquals("1", mapper.getByUserId(1L).getStatus());
        assertEquals("1", mapper.getByUserId(2L).getStatus());
        assertEquals(1, mapper.removeWhereUserId(3));
        assertNull(mapper.getByUserId(3L));
        assertTrue(mapper.removeBatch(Arrays.asList(first, second)) > 0);
        assertIds(mapper.findAllOrderByUserId(), 4L, 5L);
        assertTrue(mapper.removeBatchByUserId(Collections.singletonList(user(4L, null))) > 0);
        assertIds(mapper.findAllOrderByUserId(), 5L);
    }

    @Test
    void evaluatesBooleanChecksAndPreservesSqlNullSemantics() {
        FlagMapper flags = session.getMapper(FlagMapper.class);
        assertEquals(Collections.singletonList(1L), flags.listIdByEnabledIsTrueOrderById());
        // H2Dialect 将 IsFalse 渲染为 IS NOT TRUE，包含 NULL。
        assertEquals(Arrays.asList(2L, 3L), flags.listIdByEnabledIsFalseOrderById());
        assertEquals(Arrays.asList(2L, 3L), flags.listIdByEnabledNotIsTrueOrderById());
        assertEquals(Collections.singletonList(1L), flags.listIdByEnabledNotIsFalseOrderById());
        assertEquals(Collections.singletonList(3L), flags.listIdByEnabledIsNullOrderById());
        assertEquals(Arrays.asList(2L, 3L), flags.listIdOrderByIdLimitWindowDotOffsetToWindowDotCount(new Window(1, 2)));
    }

    @Test
    void rejectsHavingWithoutGroupBy() {
        Configuration invalid = newConfiguration();
        invalid.addMapper(HavingWithoutGroupByMapper.class);
        assertThrows(ParserException.class, () -> ((ConfigurationInterface) invalid).validateAllMapperMethod());
    }

    @Test
    void rejectsMissingConditionParameters() {
        Configuration invalid = newConfiguration();
        invalid.addMapper(InsufficientParametersMapper.class);
        MybatisExtException error = assertThrows(MybatisExtException.class, () -> ((ConfigurationInterface) invalid).validateAllMapperMethod());
        assertTrue(error.getMessage().contains("Insufficient parameters"));
    }

    @Test
    void rejectsUnknownPropertiesIncompleteTopAndMisorderedClauses() {
        for (Class<?> mapperType : Arrays.asList(UnknownPropertyMapper.class, IncompleteTopMapper.class, MisorderedClausesMapper.class)) {
            Configuration invalid = newConfiguration();
            invalid.addMapper(mapperType);
            assertThrows(ParserException.class, () -> ((ConfigurationInterface) invalid).validateAllMapperMethod(), mapperType.getSimpleName());
        }
    }

    private Configuration newConfiguration() {
        return ConfigurationFactory.create(new Environment("syntax", new JdbcTransactionFactory(), database), new ExtContext());
    }

    private static SysUser user(Long id, String name) {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setLoginName(name);
        return user;
    }

    private static void assertIds(List<SysUser> users, Long... expected) {
        assertEquals(Arrays.asList(expected), users.stream().map(SysUser::getUserId).collect(Collectors.toList()));
    }

    @Table(name = "syntax_flag")
    public static class Flag {

        @Column
        public Long id;

        @Column
        public Boolean enabled;
    }

    public interface FlagMapper extends ExtMapper<Flag> {

        List<Long> listIdByEnabledIsTrueOrderById();

        List<Long> listIdByEnabledIsFalseOrderById();

        List<Long> listIdByEnabledNotIsTrueOrderById();

        List<Long> listIdByEnabledNotIsFalseOrderById();

        List<Long> listIdByEnabledIsNullOrderById();

        List<Long> listIdOrderByIdLimitWindowDotOffsetToWindowDotCount(@Param("window") Window window);
    }

    public static class Window {

        public final int offset;

        public final int count;

        public Window(int offset, int count) {
            this.offset = offset;
            this.count = count;
        }
    }

    public interface HavingWithoutGroupByMapper extends ExtMapper<SysUser> {

        List<String> listStatusHavingStatus(@Param("status") String status);
    }

    public interface InsufficientParametersMapper extends ExtMapper<SysUser> {

        List<SysUser> listByUserIdBetween(long lower);
    }

    public interface UnknownPropertyMapper extends ExtMapper<SysUser> {

        List<SysUser> listByMissingProperty(String value);
    }

    public interface IncompleteTopMapper extends ExtMapper<SysUser> {

        List<SysUser> listTop();
    }

    public interface MisorderedClausesMapper extends ExtMapper<SysUser> {

        List<String> listStatusOrderByStatusGroupByStatus();
    }
}
