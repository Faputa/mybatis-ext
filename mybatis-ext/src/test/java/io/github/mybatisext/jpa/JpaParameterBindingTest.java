package io.github.mybatisext.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Filterable;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.OnlyById;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.dialect.H2Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.statement.SemanticScriptHelper;

class JpaParameterBindingTest {

    private final Configuration configuration = new Configuration();
    private final TableInfoFactory tables = new TableInfoFactory(configuration, new ExtContext());
    private final TableInfo table = tables.buildTableInfo(GenericTypeFactory.build(User.class));
    private final JpaParser parser = new JpaParser(configuration, tables);

    @Test
    void supportsScalarHandlersWithUnrelatedRawGenericInterfaces() {
        configuration.getTypeHandlerRegistry().register(RawComparable.class, ObjectTypeHandler.class);
        TableInfo scalarTable = tables.buildTableInfo(GenericTypeFactory.build(RawScalarRow.class));
        GenericMethod method = method("listByValue");

        Semantic semantic = parser.parse(scalarTable, method, method.getGenericReturnType());

        assertEquals("value", scalarTable.getNameToPropertyInfo().get("value").getColumnName());
        assertEquals("value", semantic.getWhere().getPropertyInfo().getName());
        assertEquals("param1", semantic.getWhere().getVariable().getFullName());
        assertTrue(SemanticScriptHelper.buildScript(Collections.singletonMap("", semantic), new H2Dialect()).contains("#{param1}"));
    }

    @Test
    void bindsUnnamedScalarAndCollectionUsingMybatisNames() throws Exception {
        assertEquals(Collections.singletonList("alice"), bind("listByName", "alice"));
        assertEquals(Arrays.asList("alice", "bob"), bind("listByNameIn", Arrays.asList("alice", "bob")));
        assertEquals(Collections.singletonList("alice"), bind("listByNameWithBounds", new RowBounds(), "alice"));
    }

    @Test
    void skipsSpecialParametersWhenExpandingAnEntity() throws Exception {
        assertEquals(Collections.singletonList("alice"), bind("listByNameWithEntity", new RowBounds(), new User(7L, "alice")));
    }

    @Test
    void preservesDefaultConditionsAndExplicitEntityGrouping() throws Exception {
        User user = new User(7L, "alice");
        assertEquals(new HashSet<>(Arrays.asList(7L, "alice")), new HashSet<>(bind("get", user)));
        assertEquals(new HashSet<>(bind("get", user)), new HashSet<>(bind("getNamed", user)));
        assertEquals(Collections.singletonList("alice"), bind("list", "alice"));
        assertNull(parse("getByIdGroupById").getHaving());
        assertEquals(Collections.singletonList(7L), bind("getByIdGroupById", user));
    }

    @Test
    void bindsGenericAliasesWhenActualParameterNamesAreDisabled() throws Exception {
        configuration.setUseActualParamName(false);

        assertEquals(new HashSet<>(Arrays.asList("alice", 7L)), new HashSet<>(bind("listByNameAndId", "alice", new RowBounds(), 7L)));
        assertEquals(Arrays.asList("alice", "bob"), bind("listByNameIn", Arrays.asList("alice", "bob")));
    }

    @Test
    void bindsNamedAndPositionalParametersWithoutCountingSpecialParameters() throws Exception {
        for (Condition condition : parse("listByNameAndId").getWhere().getSubConditions()) {
            assertEquals(condition.getPropertyInfo().getName().equals("id") ? "id" : "param1", condition.getVariable().getFullName());
        }
        assertEquals(new HashSet<>(Arrays.asList("alice", 7L)), new HashSet<>(bind("listByNameAndId", "alice", new RowBounds(), 7L)));
    }

    @Test
    void reservesLimitAndTopParametersBeforePositionalBinding() throws Exception {
        assertEquals("criterion", parse("listByIdLimitCount").getWhere().getVariable().getFullName());
        assertEquals(Arrays.asList(7L, 3), bind("listByIdLimitCount", 3, 7L));
        assertEquals("criterion", parse("listTopCountById").getWhere().getVariable().getFullName());
    }

    @Test
    void trailingLimitOverridesTopAndOneBeforeParameterBinding() throws Exception {
        for (String methodName : Arrays.asList("listTop10ByIdLimitCount", "listOneByIdLimitCount")) {
            Semantic semantic = parse(methodName);

            assertNull(semantic.getLimit().getRowCount());
            assertEquals("count", semantic.getLimit().getRowCountVariable().getFullName());
            assertEquals("criterion", semantic.getWhere().getVariable().getFullName());
            assertEquals(Arrays.asList(7L, 3), bind(methodName, 3, 7L));
        }
    }

    @Test
    void reservesHavingReferencesBeforeBindingWhere() throws Exception {
        Semantic semantic = parse("listByIdGroupByIdHavingNameIsPattern");

        assertEquals("criterion", semantic.getWhere().getVariable().getFullName());
        assertEquals("pattern", semantic.getHaving().getVariable().getFullName());
        assertEquals(Arrays.asList(7L, "alice"), bind("listByIdGroupByIdHavingNameIsPattern", "alice", 7L));
    }

    @Test
    void consumesEachUnnamedParameterOnceForBetween() throws Exception {
        assertEquals(Arrays.asList(2L, 9L), bind("listByIdBetween", 2L, 9L));
        MybatisExtException error = assertThrows(MybatisExtException.class, () -> parse("listByNameAndIdInsufficient"));
        assertTrue(error.getMessage().contains("Insufficient parameters"));
    }

    @Test
    void givesAndHigherPrecedenceThanOrWithoutChangingParameterOrder() throws Exception {
        Condition where = parse("listByIdLessThanOrIdGreaterThanAndName").getWhere();
        assertEquals(LogicalOperator.OR, where.getLogicalOperator());
        List<Condition> alternatives = new ArrayList<>(where.getSubConditions());
        assertEquals(2, alternatives.size());
        assertEquals(CompareOperator.LessThan, alternatives.get(0).getCompareOperator());
        Condition conjunction = alternatives.get(1);
        assertEquals(LogicalOperator.AND, conjunction.getLogicalOperator());
        List<Condition> terms = new ArrayList<>(conjunction.getSubConditions());
        assertEquals(2, terms.size());
        assertEquals(CompareOperator.GreaterThan, terms.get(0).getCompareOperator());
        assertEquals("name", terms.get(1).getPropertyInfo().getName());
        assertEquals(Arrays.asList(2L, 9L, "alice"), bind("listByIdLessThanOrIdGreaterThanAndName", 2L, 9L, "alice"));

        Condition reversed = parse("listByIdGreaterThanAndNameOrIdLessThan").getWhere();
        assertEquals(LogicalOperator.OR, reversed.getLogicalOperator());
        List<Condition> reversedAlternatives = new ArrayList<>(reversed.getSubConditions());
        assertEquals(LogicalOperator.AND, reversedAlternatives.get(0).getLogicalOperator());
        assertEquals(CompareOperator.LessThan, reversedAlternatives.get(1).getCompareOperator());
        assertEquals(Arrays.asList(2L, "alice", 9L), bind("listByIdGreaterThanAndNameOrIdLessThan", 2L, "alice", 9L));
    }

    @Test
    void preservesRepeatedConditionsUntilAndAfterParameterBinding() throws Exception {
        assertEquals(Arrays.asList(1L, 2L, 3L), bind("listByIdOrIdOrId", 1L, 2L, 3L));
        assertEquals(Arrays.asList(1L, 2L, 3L), bind("listByIdAndIdAndId", 1L, 2L, 3L));
        assertEquals(Arrays.asList(7L, 7L), bind("listByIdIsValueOrIdIsValue", 7L));
        assertEquals(2, parse("listByIdIsValueOrIdIsValue").getWhere().getSubConditions().size());
    }

    @Test
    void bindsBetweenBeforeFollowingMixedConditions() throws Exception {
        assertEquals(Arrays.asList(2L, 9L, 7L, "alice"), bind("listByIdBetweenOrIdAndName", 2L, 9L, 7L, "alice"));
    }

    @Test
    void reservesHavingAndLimitReferencesBeforeBindingMixedWhereConditions() throws Exception {
        assertEquals(Arrays.asList(2L, 9L, 3L, 10), bind("listByIdOrIdGroupByIdHavingIdGreaterThanThresholdLimitCount", 3L, 10, 2L, 9L));
    }

    @Test
    void keepsEntityAndBatchCollectionRootsCompatibleWithMybatis() throws Exception {
        User user = new User(7L, "alice");
        assertEquals("", parse("save").getParameter().getFullName());
        assertEquals("users", parse("saveBatchNamed").getParameter().getFullName());
        assertEquals("list", parse("saveBatch").getParameter().getFullName());
        assertEquals("collection", parse("saveBatchSet").getParameter().getFullName());
        assertEquals("array", parse("saveBatchArray").getParameter().getFullName());
        assertEquals(new HashSet<>(Arrays.asList(7L, "alice")), new HashSet<>(bind("save", user)));
        assertEquals(new HashSet<>(bind("save", user)), new HashSet<>(bind("saveBatch", Collections.singletonList(user))));
        assertEquals(new HashSet<>(bind("save", user)), new HashSet<>(bind("saveBatchArray", (Object) new User[]{user})));
        assertEquals(new HashSet<>(bind("save", user)), new HashSet<>(bind("saveBatchNamed", Collections.singletonList(user))));
        assertEquals(new HashSet<>(bind("save", user)), new HashSet<>(bind("saveBatchSet", Collections.singleton(user))));
        assertEquals(Collections.singletonList(7L), bind("deleteBatch", Collections.singletonList(user)));
        assertEquals(Arrays.asList(2L, 9L), bind("delete", (Object) new long[]{2L, 9L}));
    }

    @Test
    void reservesSecondVariablesBeforeBuildingDefaultConditions() throws Exception {
        assertEquals(Arrays.asList(2L, 9L), bind("listWithEarlierSecond", 9L, 2L));
        assertEquals(Arrays.asList(2L, 9L), bind("listWithLaterSecond", 2L, 9L));
    }

    @Test
    void ignoresReferencesFromConsumedSecondParameters() throws Exception {
        Set<Object> expected = new HashSet<>(Arrays.asList(2L, 9L, "alice"));
        assertEquals(expected, new HashSet<>(bind("listWithChainedSecond", 2L, 9L, "alice")));
        assertEquals(expected, new HashSet<>(bind("listWithReorderedChainedSecond", "alice", 9L, 2L)));
        assertEquals(expected, new HashSet<>(bind("deleteWithChainedSecond", 2L, 9L, "alice")));
        assertEquals(Arrays.asList(2L, 9L), bind("listWithUnusedSecondAnnotation", 9L, 2L));
        assertEquals(Arrays.asList(2L, 9L), bind("listWithAnnotatedNestedSecond", new Range(9L), 2L));
    }

    @Test
    void rejectsCyclicSecondReferencesInsteadOfDroppingConditions() {
        for (String fixture : Arrays.asList("listWithCyclicSecond", "listWithCyclicSecondAndName", "deleteWithCyclicSecond")) {
            MybatisExtException error = assertThrows(MybatisExtException.class, () -> parse(fixture));
            assertTrue(error.getMessage().contains("Cyclic second-variable references"), error.getMessage());
            assertTrue(error.getMessage().contains("id") && error.getMessage().contains("end"), error.getMessage());
        }
    }

    @Test
    void allowsUsingThePrimaryParameterAsItsOwnSecondVariable() throws Exception {
        assertEquals(Arrays.asList(7L, 7L), bind("listWithSelfSecond", 7L));
    }

    @Test
    void resolvesLazyNestedSecondVariablesAndRejectsEmptySegments() throws Exception {
        assertEquals(Arrays.asList(2L, 9L), bind("listWithNestedSecond", new Range(9L), 2L));
        MybatisExtException trailing = assertThrows(MybatisExtException.class, () -> parse("listWithTrailingSecondPath"));
        assertTrue(trailing.getMessage().contains("range."));
        MybatisExtException empty = assertThrows(MybatisExtException.class, () -> parse("listWithEmptySecondSegment"));
        assertTrue(empty.getMessage().contains("range..end"));
    }

    @Test
    void rejectsConflictingExplicitNamesAndGenericAliases() {
        MybatisExtException alias = assertThrows(MybatisExtException.class, () -> parse("listByNameAndIdWithAliasConflict"));
        assertTrue(alias.getMessage().contains("Conflicting parameter name 'param2'"));
        MybatisExtException duplicate = assertThrows(MybatisExtException.class, () -> parse("listByNameAndIdWithDuplicateNames"));
        assertTrue(duplicate.getMessage().contains("Conflicting parameter name 'value'"));
    }

    private GenericMethod method(String name) {
        for (GenericMethod method : GenericTypeFactory.build(Mapper.class).getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new AssertionError("未找到测试方法：" + name);
    }

    private Semantic parse(String fixtureName) {
        GenericMethod method = method(fixtureName);
        // With 和 Insufficient 仅用于区分测试签名，不属于被测方法语法。
        String name = fixtureName.replaceAll("With.*|Insufficient|Named|Set|Array", "");
        return parser.parse(table, name, method.getParameters(), method.getGenericReturnType());
    }

    /** 生成真实动态 SQL，并用 MyBatis 的参数解析器和处理器验证最终 JDBC 值。 */
    private List<Object> bind(String fixtureName, Object... args) throws Exception {
        Semantic semantic = parse(fixtureName);
        String script = SemanticScriptHelper.buildScript(Collections.singletonMap("", semantic), new H2Dialect());
        SqlSource source = configuration.getDefaultScriptingLanguageInstance().createSqlSource(configuration, script, Object.class);
        Object parameters = new ParamNameResolver(configuration, method(fixtureName).getMethod()).getNamedParams(args);
        BoundSql sql = source.getBoundSql(parameters);
        MappedStatement statement = new MappedStatement.Builder(configuration, fixtureName, source, SqlCommandType.SELECT).build();
        List<Object> values = new ArrayList<>();
        PreparedStatement jdbc = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class}, (proxy, method, arguments) -> {
            if (method.getName().startsWith("set")) {
                values.add(arguments[1]);
            }
            return null;
        });
        new DefaultParameterHandler(statement, parameters, sql).setParameters(jdbc);
        return values;
    }

    interface Mapper {

        List<RawScalarRow> listByValue(RawComparable value);

        List<User> listByName(String value);

        List<User> list(@Param("name") String value);

        User get(User criteria);

        User getNamed(@Param("criteria") User criteria);

        User getByIdGroupById(User criteria);

        List<User> listByNameIn(List<String> values);

        List<User> listByNameWithBounds(RowBounds bounds, String value);

        List<User> listByNameWithEntity(RowBounds bounds, User criteria);

        List<User> listByNameAndId(String value, RowBounds bounds, @Param("id") long id);

        List<User> listByIdLimitCount(@Param("count") int count, @Param("criterion") long criterion);

        List<User> listTopCountById(@Param("count") int count, @Param("criterion") long criterion);

        List<User> listTop10ByIdLimitCount(@Param("count") int count, @Param("criterion") long criterion);

        List<User> listOneByIdLimitCount(@Param("count") int count, @Param("criterion") long criterion);

        List<User> listByIdGroupByIdHavingNameIsPattern(@Param("pattern") String pattern, @Param("criterion") long criterion);

        List<User> listByIdBetween(long start, long end);

        List<User> listByIdLessThanOrIdGreaterThanAndName(long lower, long upper, String label);

        List<User> listByIdGreaterThanAndNameOrIdLessThan(long lower, String label, long upper);

        List<User> listByIdOrIdOrId(long first, long second, long third);

        List<User> listByIdAndIdAndId(long first, long second, long third);

        List<User> listByIdIsValueOrIdIsValue(@Param("value") long value);

        List<User> listByIdBetweenOrIdAndName(long lower, long upper, long id, String label);

        List<User> listByIdOrIdGroupByIdHavingIdGreaterThanThresholdLimitCount(@Param("threshold") long threshold, @Param("count") int count,
                @Param("first") long first, @Param("second") long second);

        List<User> listByNameAndIdInsufficient(String value);

        List<User> listWithEarlierSecond(@Param("end") long end, @Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "end") long start);

        List<User> listWithLaterSecond(@Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "end") long start, @Param("end") long end);

        List<User> listWithChainedSecond(@Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "end") long start,
                @Param("end") @Filterable(operator = CompareOperator.Between, secondVariable = "name") long end, @Param("name") String name);

        List<User> listWithReorderedChainedSecond(@Param("name") String name, @Param("end") @Filterable(operator = CompareOperator.Between, secondVariable = "name") long end,
                @Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "end") long start);

        int deleteWithChainedSecond(@Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "end") long start,
                @Param("end") @Filterable(operator = CompareOperator.Between, secondVariable = "name") long end, @Param("name") String name);

        List<User> listWithCyclicSecond(@Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "end") long start,
                @Param("end") @Filterable(operator = CompareOperator.Between, secondVariable = "id") long end);

        List<User> listWithCyclicSecondAndName(@Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "end") long start,
                @Param("end") @Filterable(operator = CompareOperator.Between, secondVariable = "id") long end, @Param("name") String name);

        int deleteWithCyclicSecond(@Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "end") long start,
                @Param("end") @Filterable(operator = CompareOperator.Between, secondVariable = "id") long end);

        List<User> listWithSelfSecond(@Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "id") long value);

        List<User> listWithUnusedSecondAnnotation(@Param("end") @Filterable(operator = CompareOperator.Between, secondVariable = "missing.path") long end,
                @Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "end") long start);

        List<User> listWithAnnotatedNestedSecond(@Param("range") @Filterable(operator = CompareOperator.Between, secondVariable = "id") Range range,
                @Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "range.end") long start);

        List<User> listWithNestedSecond(@Param("range") Range range, @Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "range.end") long start);

        List<User> listWithTrailingSecondPath(@Param("range") Range range, @Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "range.") long start);

        List<User> listWithEmptySecondSegment(@Param("range") Range range, @Param("id") @Filterable(operator = CompareOperator.Between, secondVariable = "range..end") long start);

        List<User> listByNameAndIdWithAliasConflict(@Param("param2") String name, long id);

        List<User> listByNameAndIdWithDuplicateNames(@Param("value") String name, @Param("value") long id);

        int save(User user);

        int saveBatch(List<User> users);

        int saveBatchNamed(@Param("users") List<User> users);

        int saveBatchSet(Set<User> users);

        int saveBatchArray(User[] users);

        int deleteBatch(@OnlyById List<User> users);

        int delete(@Param("id") long[] ids);
    }

    @SuppressWarnings("rawtypes")
    static class RawComparable implements Comparable {

        @Override
        public int compareTo(Object other) {
            return 0;
        }
    }

    @Table
    static class RawScalarRow {

        @Column
        private RawComparable value;
    }

    static class Range {

        private final long end;

        Range(long end) {
            this.end = end;
        }
    }

    @Table
    static class User {

        @Id
        @Column
        private final Long id;

        @Column
        private final String name;

        User(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
