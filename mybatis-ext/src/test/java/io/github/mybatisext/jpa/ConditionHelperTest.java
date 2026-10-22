package io.github.mybatisext.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Filterable;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.annotation.TestMode;
import io.github.mybatisext.dialect.H2Dialect;
import io.github.mybatisext.fixture.permission.TablePermission;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

public class ConditionHelperTest {

    @Test
    public void buildsConditionTreeFromTableInfo() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtContext extContext = new ExtContext();
        Configuration configuration = ConfigurationFactory.create(environment, extContext);
        GenericType genericType = GenericTypeFactory.build(TablePermission.class);
        TableInfo tableInfo = new TableInfoFactory(configuration, extContext).buildTableInfo(genericType);
        Condition condition = ConditionHelper.buildForTableInfo(tableInfo, false, "pt");
        assertNotNull(condition);
        assertEquals(ConditionType.COMPLEX, condition.getType());
        assertFalse(condition.getSubConditions().isEmpty());
    }

    @TableRef(TablePermission.class)
    static class A {
    }

    @TableRef(TablePermission.class)
    static class B {
    }

    @Test
    public void annotationsWithEqualAttributesHaveEqualHashCodes() {
        TableRef tableRef = A.class.getAnnotation(TableRef.class);
        TableRef tableRef2 = B.class.getAnnotation(TableRef.class);
        Set<TableRef> set = new HashSet<>();
        set.add(tableRef);
        set.add(tableRef2);
        assertEquals(1, set.size());
    }

    @Test
    void preservesConditionOrderAndDuplicatesWhileComparingAsSets() {
        Condition first = expression("n = 1", false);
        Condition second = expression("n = 2", false);
        Condition ordered = negated(first, second, first);
        Condition reordered = negated(second, first);

        assertEquals(Arrays.asList(first, second, first), new ArrayList<>(ordered.getSubConditions()));
        assertEquals(ordered, reordered);
        assertEquals(reordered, ordered);
        assertEquals(ordered.hashCode(), reordered.hashCode());

        Condition parent = negated(ordered);
        Condition otherParent = negated(reordered);
        assertEquals(parent, otherParent);
        assertEquals(parent.hashCode(), otherParent.hashCode());
    }

    @Test
    void buildsObjectConditionsFromFilterableMetadata() {
        ExtContext extContext = new ExtContext();
        extContext.setDefaultFilterable(false);
        TableInfo tableInfo = buildTableInfo(ExplicitFilterCriteria.class, extContext);

        assertNull(tableInfo.get("unfiltered").getFilterableInfo());
        assertEquals(TestMode.NotEmpty, tableInfo.get("name").getFilterableInfo().getTestMode());
        assertEquals(CompareOperator.Like, tableInfo.get("name").getFilterableInfo().getOperator());
        assertTrue(tableInfo.get("name").getFilterableInfo().isIgnorecase());
        assertEquals(LogicalOperator.OR, tableInfo.get("statuses").getFilterableInfo().getLogicalOperator());
        assertTrue(tableInfo.get("statuses").getFilterableInfo().isNot());
        assertEquals("upper", tableInfo.get("lower").getFilterableInfo().getSecondVariable());

        Condition condition = ConditionHelper.buildForTableInfo(tableInfo, false, "query");
        Condition simplified = ConditionHelper.simplifyCondition(condition);
        assertNotNull(simplified);
        String where = ConditionHelper.toWhere(simplified, new H2Dialect());
        assertTrue(where.contains("@io.github.mybatisext.ognl.Ognl@isNotEmpty(query.name)"));
        assertTrue(where.contains("UPPER(fc.name) LIKE"));
        assertTrue(where.contains("NOT fc.status IN"));
        assertTrue(where.contains("query.lower") && where.contains("query.upper"));
        assertTrue(where.contains("test=\"query.custom != null\""));
        assertTrue(where.contains("fc.custom = #{query.custom"));
        assertFalse(where.contains("unfiltered"));
        assertFalse(where.contains("disabled"));
    }

    @Test
    void defaultFilterableControlsUnannotatedObjectProperties() {
        ExtContext disabled = new ExtContext();
        disabled.setDefaultFilterable(false);

        assertNotNull(buildTableInfo(DefaultFilterCriteria.class, new ExtContext()).get("value").getFilterableInfo());
        assertNull(buildTableInfo(DisabledFilterCriteria.class, disabled).get("value").getFilterableInfo());
    }

    @Test
    void executesDynamicHavingAndOmitsDisabledCondition() throws Exception {
        Condition condition = expression("n = 1", true);
        String having = ConditionHelper.toHaving(condition, new H2Dialect());
        assertEquals(Arrays.asList(1), executeCondition(having, true, true));
        assertEquals(Arrays.asList(1, 2, 3), executeCondition(having, false, true));
    }

    @Test
    void preservesLogicalOperatorInDynamicHaving() throws Exception {
        List<Condition> conditions = Arrays.asList(expression("n = 1", true), expression("n = 3", true));
        assertEquals(Arrays.asList(1, 3), executeCondition(ConditionHelper.toHaving(conditions, LogicalOperator.OR, new H2Dialect()), true, true));
        assertEquals(Collections.emptyList(), executeCondition(ConditionHelper.toHaving(conditions, LogicalOperator.AND, new H2Dialect()), true, true));
        assertEquals(Arrays.asList(1, 2, 3), executeCondition(ConditionHelper.toHaving(conditions, LogicalOperator.OR, new H2Dialect()), false, true));
    }

    @Test
    void preservesNegationWhenSimplifyingSingleChild() throws Exception {
        Condition condition = negated(expression("n = 1", false));
        Condition simplified = ConditionHelper.simplifyCondition(condition);
        assertEquals(Arrays.asList(2, 3), executeCondition(ConditionHelper.toWhere(simplified, new H2Dialect()), true, false));
    }

    @Test
    void preservesNegationOfMultipleDynamicChildren() throws Exception {
        Condition condition = negated(expression("n >= 1", true), expression("n &lt;= 2", true));
        String where = ConditionHelper.toWhere(condition, new H2Dialect());
        assertEquals(Arrays.asList(3), executeCondition(where, true, false));
        assertEquals(Arrays.asList(1, 2, 3), executeCondition(where, false, false));
    }

    @Test
    void preservesNegationWithOuterTestAndSingleStaticChild() throws Exception {
        Condition condition = negated(expression("n = 1", false));
        condition.setTestTemplate("enabled");
        String where = ConditionHelper.toWhere(condition, new H2Dialect());
        assertEquals(Arrays.asList(2, 3), executeCondition(where, true, false));
        assertEquals(Arrays.asList(1, 2, 3), executeCondition(where, false, false));
    }

    @Test
    void preservesNegationInHaving() throws Exception {
        Condition condition = negated(expression("n >= 1", true), expression("n &lt;= 2", true));
        String having = ConditionHelper.toHaving(condition, new H2Dialect());
        assertEquals(Arrays.asList(3), executeCondition(having, true, true));
        assertEquals(Arrays.asList(1, 2, 3), executeCondition(having, false, true));
    }

    private Condition expression(String expression, boolean dynamic) {
        Condition condition = new Condition(ConditionType.BASIC);
        condition.setExprTemplate(expression);
        condition.setLogicalOperator(LogicalOperator.AND);
        if (dynamic) {
            condition.setTestTemplate("enabled");
        }
        return condition;
    }

    private Condition negated(Condition... children) {
        Condition condition = new Condition(ConditionType.COMPLEX);
        condition.setLogicalOperator(LogicalOperator.AND);
        condition.setNot(true);
        condition.getSubConditions().addAll(Arrays.asList(children));
        return condition;
    }

    private List<Integer> executeCondition(String condition, boolean enabled, boolean having) throws Exception {
        String script = "<script>SELECT n FROM (VALUES (1), (2), (3)) t(n) " + (having ? "GROUP BY n " : "") + condition + " ORDER BY n</script>";
        String sql = new XMLLanguageDriver().createSqlSource(new Configuration(), script, Object.class).getBoundSql(Collections.singletonMap("enabled", enabled)).getSql();
        List<Integer> rows = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:condition_script"); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                rows.add(result.getInt(1));
            }
        }
        return rows;
    }

    private TableInfo buildTableInfo(Class<?> type, ExtContext extContext) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:condition_metadata;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        Environment environment = new Environment("development", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = ConfigurationFactory.create(environment, extContext);
        return new TableInfoFactory(configuration, extContext).buildTableInfo(GenericTypeFactory.build(type));
    }

    @Table(name = "filter_criteria", alias = "fc")
    static class ExplicitFilterCriteria {

        @Column
        private String unfiltered;
        @Column
        @Filterable(test = TestMode.NotEmpty, operator = CompareOperator.Like, ignorecase = true)
        private String name;
        @Column(name = "status")
        @Filterable(operator = CompareOperator.In, logicalOperator = LogicalOperator.OR, not = true)
        private List<String> statuses;
        @Column
        @Filterable(test = TestMode.False)
        private String disabled;
        @Column
        @Filterable(operator = CompareOperator.Between, secondVariable = "upper")
        private Long lower;
        @Column
        @Filterable(test = TestMode.False)
        private Long upper;
        @Column
        @Filterable(testTemplate = "{variable} != null", exprTemplate = "{propertyInfo} = {variable.##placeholder}")
        private String custom;
    }

    @Table
    static class DefaultFilterCriteria {

        @Column
        private String value;
    }

    @Table
    static class DisabledFilterCriteria {

        @Column
        private String value;
    }
}
