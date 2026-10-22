package io.github.mybatisext.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TestMode;
import io.github.mybatisext.dialect.H2Dialect;
import io.github.mybatisext.fixture.permission.TablePermission;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericTypeFactory;

class ConditionRenderingTest {

    private final Configuration configuration = new Configuration();
    private final H2Dialect dialect = new H2Dialect();
    private final TableInfo table = new TableInfoFactory(configuration, new ExtContext()).buildTableInfo(GenericTypeFactory.build(Item.class));

    @Test
    void rendersFixedWhereAndHavingWithoutDynamicTags() {
        Condition condition = group(comparison(CompareOperator.IsNull, "first"), comparison(CompareOperator.IsNotNull, "second"));
        String where = ConditionHelper.toWhere(condition, dialect);
        assertTrue(Arrays.asList("WHERE t.name IS NULL AND t.name IS NOT NULL", "WHERE t.name IS NOT NULL AND t.name IS NULL").contains(where), where);
        assertEquals(where.replaceFirst("WHERE", "HAVING"), ConditionHelper.toHaving(condition, dialect));
        assertEquals("WHERE t.name IS NULL", ConditionHelper.toWhere(comparison(CompareOperator.IsNull, "term"), dialect));
        assertEquals("HAVING t.name IS NULL OR t.name IS NOT NULL", ConditionHelper.toHaving(Arrays.asList(comparison(CompareOperator.IsNull, "first"), comparison(CompareOperator.IsNotNull, "second")), LogicalOperator.OR, dialect));
    }

    @Test
    void rendersUnconditionalInWithoutClauseTrim() throws Exception {
        Condition in = comparison(CompareOperator.In, "terms");
        in.setVariable(new Variable("terms", GenericTypeFactory.build(String[].class)));
        Condition condition = group(in);
        String where = ConditionHelper.toWhere(condition, dialect);
        String having = ConditionHelper.toHaving(condition, dialect);
        assertTrue(where.startsWith("WHERE t.name IN <foreach"), where);
        assertTrue(having.startsWith("HAVING t.name IN <foreach"), having);
        assertFalse(where.contains("<where>"), where);
        assertFalse(having.contains("<trim"), having);
        Map<String, Object> parameters = Collections.singletonMap("terms", Arrays.asList("abc", "def"));
        assertEquals(Arrays.asList("abc", "def"), bindings(where, parameters));
        assertEquals(Arrays.asList("abc", "def"), bindings(having, parameters));
    }

    @Test
    void keepsTestedInDynamicWhenCollectionIsEmpty() {
        Condition condition = comparison(CompareOperator.In, "terms");
        condition.setVariable(new Variable("terms", GenericTypeFactory.build(String[].class)));
        condition.setTestMode(TestMode.NotEmpty);
        Map<String, Object> parameters = Collections.singletonMap("terms", Collections.emptyList());
        assertEquals("SELECT n FROM item", sql(ConditionHelper.toWhere(condition, dialect), parameters).getSql().trim());
        assertEquals("SELECT n FROM item", sql(ConditionHelper.toHaving(condition, dialect), parameters).getSql().trim());
    }

    @Test
    void preservesFixedGroupingAndNegation() {
        Condition alternatives = group(comparison(CompareOperator.IsNull, "first"), comparison(CompareOperator.IsNotNull, "second"));
        alternatives.setLogicalOperator(LogicalOperator.OR);
        alternatives.setNot(true);
        List<Condition> conditions = Arrays.asList(alternatives, comparison(CompareOperator.IsNull, "third"));
        String where = ConditionHelper.toWhere(conditions, LogicalOperator.AND, dialect);
        assertTrue(Arrays.asList("WHERE NOT (t.name IS NULL OR t.name IS NOT NULL) AND t.name IS NULL", "WHERE NOT (t.name IS NOT NULL OR t.name IS NULL) AND t.name IS NULL").contains(where), where);
        assertEquals(where.replaceFirst("WHERE", "HAVING"), ConditionHelper.toHaving(conditions, LogicalOperator.AND, dialect));
    }

    @Test
    void preservesFixedAssociationScope() {
        TableInfo permissions = new TableInfoFactory(configuration, new ExtContext()).buildTableInfo(GenericTypeFactory.build(TablePermission.class));
        Condition association = group(comparison(CompareOperator.IsNull, "term"));
        association.setPropertyInfo(permissions.get("rowPermissions2"));
        String where = ConditionHelper.toWhere(association, dialect);
        assertTrue(where.startsWith("WHERE EXISTS (SELECT 1 FROM row_permission"), where);
        assertFalse(where.contains("<"), where);
        String sql = sql(where, Collections.emptyMap()).getSql();
        assertTrue(sql.contains("tp.role_id"), sql);
        assertTrue(sql.contains("t.name IS NULL"), sql);
        assertEquals(where.replaceFirst("WHERE", "HAVING"), ConditionHelper.toHaving(association, dialect));
    }

    @Test
    void preservesAssociationExpressionPrecedence() {
        TableInfo permissions = new TableInfoFactory(configuration, new ExtContext()).buildTableInfo(GenericTypeFactory.build(TablePermission.class));
        Condition association = expression("1 = 0 OR 1 = 1");
        association.setPropertyInfo(permissions.get("rowPermissions2"));
        String where = ConditionHelper.toWhere(association, dialect);
        String sql = sql(where, Collections.emptyMap()).getSql().replaceAll("\\s+", " ");
        assertTrue(sql.matches(".*AND \\(\\s*1 = 0 OR 1 = 1\\s*\\).*"), sql);
        assertTrue(ConditionHelper.toHaving(association, dialect).startsWith("<trim prefix=\"HAVING\""));
    }

    @Test
    void omitsDisabledConditions() {
        Condition disabled = expression("n = 2");
        disabled.setTestMode(TestMode.False);
        assertEquals("SELECT n FROM item", sql(ConditionHelper.toWhere(disabled, dialect), Collections.emptyMap()).getSql().trim());
        assertEquals("SELECT n FROM item", sql(ConditionHelper.toHaving(disabled, dialect), Collections.emptyMap()).getSql().trim());
    }

    @Test
    void keepsCustomExpressionsDynamicAndOverridesChildren() {
        Condition condition = group(dynamic("n = 1"));
        condition.setExprTemplate("n = 3");
        for (Condition custom : Arrays.asList(condition, expression("n = 3"))) {
            String where = ConditionHelper.toWhere(custom, dialect);
            String having = ConditionHelper.toHaving(custom, dialect);
            assertTrue(where.startsWith("<where>"), where);
            assertTrue(having.startsWith("<trim prefix=\"HAVING\""), having);
            assertEquals("SELECT n FROM item WHERE n = 3", sql(where, Collections.emptyMap()).getSql().replaceAll("\\s+", " ").trim());
            assertEquals("SELECT n FROM item HAVING n = 3", sql(having, Collections.emptyMap()).getSql().replaceAll("\\s+", " ").trim());
        }
    }

    @Test
    void omitsNestedDynamicGroupBeforeFixedCondition() {
        Condition condition = group(group(dynamic("n = 1"), dynamic("n = 2")), expression("n = 3"));
        Map<String, Object> parameters = Collections.singletonMap("enabled", false);
        assertEquals("SELECT n FROM item WHERE n = 3", sql(ConditionHelper.toWhere(condition, dialect), parameters).getSql().replaceAll("\\s+", " ").trim());
        assertEquals("SELECT n FROM item HAVING n = 3", sql(ConditionHelper.toHaving(condition, dialect), parameters).getSql().replaceAll("\\s+", " ").trim());
    }

    @Test
    void omitsEmptyCollectionsAndConditionLists() {
        Condition collection = group(expression("n = {variable.##placeholder}"));
        collection.getSubConditions().iterator().next().setVariable(new Variable("item", GenericTypeFactory.build(Integer.class)));
        collection.setCollectionVariable(new Variable("numbers", GenericTypeFactory.build(Integer[].class)));
        collection.setVariable(new Variable("item", GenericTypeFactory.build(Integer.class)));
        Map<String, Object> parameters = Collections.singletonMap("numbers", Collections.emptyList());
        assertEquals("SELECT n FROM item", sql(ConditionHelper.toWhere(collection, dialect), parameters).getSql().trim());
        assertEquals("SELECT n FROM item", sql(ConditionHelper.toHaving(collection, dialect), parameters).getSql().trim());
        assertEquals("SELECT n FROM item", sql(ConditionHelper.toWhere(Collections.emptyList(), LogicalOperator.AND, dialect), parameters).getSql().trim());
        assertEquals("SELECT n FROM item", sql(ConditionHelper.toHaving(group(), dialect), parameters).getSql().trim());
    }

    @Test
    void preservesExpressionOnSingleChildAndEmptyGroups() {
        Condition group = group(expression("n = 1"));
        group.setExprTemplate("n = 3");
        assertSame(group, ConditionHelper.simplifyCondition(group));

        Condition empty = group();
        empty.setExprTemplate("n = 3");
        assertSame(empty, ConditionHelper.simplifyCondition(empty));
        assertTrue(sql(ConditionHelper.toWhere(group, dialect), Collections.emptyMap()).getSql().contains("n = 3"));
    }

    @Test
    void preservesNestedExpressionDuringRendering() {
        Condition inner = group(expression("n = 1"), expression("n = 2"));
        inner.setExprTemplate("n = 3");
        Condition outer = group(inner, expression("n = 4"));
        outer.setTestTemplate("enabled");
        String sql = sql(ConditionHelper.toWhere(outer, dialect), Collections.singletonMap("enabled", true)).getSql();
        assertTrue(sql.contains("n = 3"), sql);
        assertFalse(sql.contains("n = 1"), sql);
    }

    @Test
    void preservesCollectionOnSingleChildGroups() throws Exception {
        Condition child = expression("n = {variable.##placeholder}");
        child.setVariable(new Variable("item", GenericTypeFactory.build(Integer.class)));
        Condition group = group(child);
        group.setCollectionVariable(new Variable("numbers", GenericTypeFactory.build(Integer[].class)));
        group.setVariable(new Variable("item", GenericTypeFactory.build(Integer.class)));
        assertSame(group, ConditionHelper.simplifyCondition(group));
        assertEquals(Arrays.asList(1, 3), bindings(ConditionHelper.toWhere(group, dialect), Collections.singletonMap("numbers", Arrays.asList(1, 3))));
    }

    @Test
    void removesEmptyDynamicCollectionGroups() {
        Condition root = group(dynamic("n = 1"));
        root.setCollectionVariable(new Variable("numbers", GenericTypeFactory.build(Integer[].class)));
        root.setVariable(new Variable("item", GenericTypeFactory.build(Integer.class)));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("numbers", Arrays.asList(1, 3));
        parameters.put("enabled", false);
        assertEquals("SELECT n FROM item", sql(ConditionHelper.toWhere(root, dialect), parameters).getSql().trim());
        assertEquals("SELECT n FROM item", sql(ConditionHelper.toHaving(root, dialect), parameters).getSql().trim());
    }

    @Test
    void removesEmptyNestedDynamicWhereAndHaving() {
        Condition first = group(dynamic("n = 1"), dynamic("n = 2"));
        Condition second = group(dynamic("n = 3"), dynamic("n = 4"));
        second.setLogicalOperator(LogicalOperator.OR);
        Condition root = group(first, second);
        Map<String, Object> parameters = Collections.singletonMap("enabled", false);
        assertEquals("SELECT n FROM item", sql(ConditionHelper.toWhere(root, dialect), parameters).getSql().trim());
        assertEquals("SELECT n FROM item", sql(ConditionHelper.toHaving(root, dialect), parameters).getSql().trim());
    }

    @Test
    void preservesAssociationScopeDuringSimplificationAndRendering() {
        TableInfo permissions = new TableInfoFactory(configuration, new ExtContext()).buildTableInfo(GenericTypeFactory.build(TablePermission.class));
        Condition association = group(dynamic("1 = 1"));
        association.setPropertyInfo(permissions.get("rowPermissions2"));
        assertSame(association, ConditionHelper.simplifyCondition(association));
        Condition outer = group(association, expression("n = 4"));
        String script = ConditionHelper.toWhere(outer, dialect);
        String enabled = sql(script, Collections.singletonMap("enabled", true)).getSql();
        assertTrue(enabled.contains("EXISTS (SELECT 1 FROM row_permission"), enabled);
        assertTrue(enabled.contains("tp.role_id"), enabled);
        String disabled = sql(script, Collections.singletonMap("enabled", false)).getSql();
        assertFalse(disabled.contains("EXISTS"), disabled);
        assertTrue(disabled.contains("n = 4"), disabled);

        // 外层测试加单个静态子条件也必须保留关联查询作用域。
        Condition testedAssociation = group(expression("1 = 1"));
        testedAssociation.setPropertyInfo(permissions.get("rowPermissions2"));
        testedAssociation.setTestTemplate("enabled");
        String tested = sql(ConditionHelper.toWhere(testedAssociation, dialect), Collections.singletonMap("enabled", true)).getSql();
        assertTrue(tested.contains("EXISTS (SELECT 1 FROM row_permission"), tested);
    }

    @Test
    void simplifiesChildrenBeforeCollapsingTransparentGroups() {
        Condition kept = expression("n = 1");
        Condition disabled = expression("n = 2");
        disabled.setTestMode(TestMode.False);
        Condition root = group(group(kept, disabled));
        assertSame(kept, ConditionHelper.simplifyCondition(root));
    }

    @Test
    void keepsJdbcTypesLocalToEachCondition() {
        Variable value = new Variable("term", GenericTypeFactory.build(String.class));
        PropertyInfo varchar = new PropertyInfo();
        varchar.setJdbcType(JdbcType.VARCHAR);
        PropertyInfo chars = new PropertyInfo();
        chars.setJdbcType(JdbcType.CHAR);
        Condition first = new Condition(ConditionType.BASIC);
        first.setPropertyInfo(varchar);
        first.setVariable(value);
        Condition second = new Condition(ConditionType.BASIC);
        second.setPropertyInfo(chars);
        second.setVariable(value);
        assertEquals(JdbcType.VARCHAR, first.getVariable().getJdbcType());
        assertEquals(JdbcType.CHAR, second.getVariable().getJdbcType());
        assertNull(value.getJdbcType());
    }

    @Test
    void separatesDifferentTransformationsOfOneParameter() throws Exception {
        Condition like = comparison(CompareOperator.Like, "term");
        Condition starts = comparison(CompareOperator.StartWith, "term");
        String clause = ConditionHelper.toWhere(Arrays.asList(like, starts), LogicalOperator.AND, dialect);
        assertEquals(Arrays.asList("%abc%", "abc%"), bindings(clause, Collections.singletonMap("term", "abc")));
    }

    @Test
    void separatesWhereAndHavingBindings() throws Exception {
        String where = ConditionHelper.toWhere(comparison(CompareOperator.Like, "term"), dialect);
        String having = ConditionHelper.toHaving(comparison(CompareOperator.EndWith, "term"), dialect);
        assertEquals(Arrays.asList("%abc%", "%abc"), bindings(where + " GROUP BY t.name " + having, Collections.singletonMap("term", "abc")));
    }

    private Condition comparison(CompareOperator operator, String variable) {
        Condition condition = new Condition(ConditionType.BASIC);
        condition.setPropertyInfo(table.get("name"));
        condition.setVariable(new Variable(variable, GenericTypeFactory.build(String.class)));
        condition.setCompareOperator(operator);
        return condition;
    }

    private Condition expression(String expression) {
        Condition condition = new Condition(ConditionType.BASIC);
        condition.setExprTemplate(expression);
        return condition;
    }

    private Condition dynamic(String expression) {
        Condition condition = expression(expression);
        condition.setTestTemplate("enabled");
        return condition;
    }

    private Condition group(Condition... children) {
        Condition condition = new Condition(ConditionType.COMPLEX);
        condition.setLogicalOperator(LogicalOperator.AND);
        condition.getSubConditions().addAll(Arrays.asList(children));
        return condition;
    }

    private BoundSql sql(String clause, Map<String, Object> parameters) {
        String script = "<script>SELECT n FROM item " + clause + "</script>";
        return configuration.getDefaultScriptingLanguageInstance().createSqlSource(configuration, script, Object.class).getBoundSql(parameters);
    }

    private List<Object> bindings(String clause, Map<String, Object> parameters) throws Exception {
        BoundSql sql = sql(clause, parameters);
        MappedStatement statement = new MappedStatement.Builder(configuration, "probe", parameter -> sql, SqlCommandType.SELECT).build();
        List<Object> values = new ArrayList<>();
        PreparedStatement jdbc = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
            if (method.getName().startsWith("set")) {
                values.add(args[1]);
            }
            return null;
        });
        new DefaultParameterHandler(statement, parameters, sql).setParameters(jdbc);
        return values;
    }

    @Table(alias = "t")
    static class Item {

        @Column
        private String name;
    }
}
