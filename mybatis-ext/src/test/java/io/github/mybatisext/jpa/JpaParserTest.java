package io.github.mybatisext.jpa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ConfigurationFactory;
import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.dialect.H2Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.fixture.permission.TablePermission;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.statement.ParameterSignature;
import io.github.mybatisext.statement.ParameterSignatureHelper;
import io.github.mybatisext.statement.SemanticScriptHelper;

public class JpaParserTest {

    private final Configuration configuration;
    private final TableInfo tableInfo;
    private final JpaParser jpaParser;

    JpaParserTest() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        ExtContext extContext = new ExtContext();
        configuration = ConfigurationFactory.create(environment, extContext);
        GenericType genericType = GenericTypeFactory.build(TablePermission.class);
        tableInfo = TableInfoFactory.buildTableInfo(genericType, configuration, extContext);
        jpaParser = new JpaParser(configuration, extContext);
    }

    @Test
    public void buildsScriptsForCoreMethodSemantics() {
        Map<String, String> scripts = buildScripts();

        String save = scripts.get("save");
        assertTrue(save.contains("@io.github.mybatisext.ognl.Ognl@getUuid(tableId)"));
        assertFalse(save.contains("getUuid('tableId')"));

        String overloadedDelete = scripts.get("deleteByDataSourceName");
        assertTrue(overloadedDelete.contains("io.github.mybatisext.fixture.permission.TablePermission|"));
        assertTrue(overloadedDelete.contains("java.lang.String|"));
        assertTrue(overloadedDelete.contains("WHERE EXISTS"));

        String topInOrderBy = scripts.get("getDistinctTop10ByRoleId$AndTableIdAndColumnPermissionsDotColumnNameInXyz$OrderByCreatedAt");
        assertTrue(topInOrderBy.contains("SELECT DISTINCT"));
        assertTrue(topInOrderBy.contains("collection=\"xyz\""));
        assertTrue(topInOrderBy.contains("ORDER BY tp.created_at LIMIT 10"));

        String updateByRelation = scripts.get("updateIgnoreNullByDataSourceName");
        assertTrue(updateByRelation.contains("<set>"));
        assertTrue(updateByRelation.contains("WHERE EXISTS"));
        assertTrue(updateByRelation.contains("t0.name = #{dataSourceName}"));

        String queryByParameterNames = scripts.get("get");
        assertTrue(queryByParameterNames.contains("tp.table_id = #{tableId}"));
        assertTrue(queryByParameterNames.contains("tp.role_id = #{roleId}"));

        String explicitUpdate = scripts.get("updatePermissionTypeAndUpdatedAtByRoleId");
        assertTrue(explicitUpdate.contains("tp.permission_type = #{permissionType}"));
        assertTrue(explicitUpdate.contains("tp.updated_at = #{updatedAt}"));
        assertFalse(explicitUpdate.contains("tp.created_at ="));
        assertTrue(explicitUpdate.contains("tp.role_id = #{roleId}"));

        String ignorecaseLike = scripts.get("listByPermissionTypeIgnorecaseLike");
        assertTrue(ignorecaseLike.contains("UPPER(tp.permission_type) LIKE"));
        assertTrue(scripts.get("listByPermissionTypeStartWith").contains("value=\"permissionType + '%'\""));
        assertTrue(scripts.get("listByPermissionTypeEndWith").contains("value=\"'%' + permissionType\""));

        String between = scripts.get("listByCreatedAtBetweenStartToEnd");
        assertTrue(between.contains("tp.created_at BETWEEN #{start") && between.contains("AND #{end"));
        assertTrue(scripts.get("listByCreatedAtLessThan").contains("tp.created_at &lt; #{createdAt"));
        assertTrue(scripts.get("listByCreatedAtLessThanEqual").contains("tp.created_at &lt;= #{createdAt"));
        assertTrue(scripts.get("listByCreatedAtGreaterThan").contains("tp.created_at &gt; #{createdAt"));
        assertTrue(scripts.get("listByCreatedAtGreaterThanEqual").contains("tp.created_at &gt;= #{createdAt"));

        String nullChecks = scripts.get("listByUpdatedAtIsNullOrCreatedAtIsNotNull");
        assertTrue(nullChecks.contains("tp.updated_at IS NULL"));
        assertTrue(nullChecks.contains("tp.created_at IS NOT NULL"));
        assertTrue(nullChecks.contains(" OR "));
        assertTrue(scripts.get("listByEnabledIsTrue").contains("tp.enabled IS TRUE"));
        assertTrue(scripts.get("listByEnabledIsFalse").contains("tp.enabled IS NOT TRUE"));

        String dynamicLimit = scripts.get("listOrderByIdLimitOffsetToRowCount");
        assertTrue(dynamicLimit.contains("LIMIT #{offset}, #{rowCount}"), dynamicLimit);
        String fixedLimit = scripts.get("listOrderByIdLimit2");
        assertTrue(fixedLimit.contains("LIMIT 2"), fixedLimit);
        String fixedOffsetLimit = scripts.get("listOrderByIdLimit2To3");
        assertTrue(fixedOffsetLimit.contains("LIMIT 2, 3"), fixedOffsetLimit);

        String deleteIn = scripts.get("deleteByTableIdIn");
        assertTrue(deleteIn.contains("tp.table_id IN"));
        assertTrue(deleteIn.contains("collection=\"tableId\""));

        String deleteNotIn = scripts.get("deleteByRoleId$AndTableIdNotIn");
        assertTrue(deleteNotIn.contains("tp.role_id = #{roleId}"));
        assertTrue(deleteNotIn.contains("NOT tp.table_id IN"), deleteNotIn);
    }

    @Test
    void rejectsScalarParametersForEntityUpdate() {
        GenericMethod method = GenericTypeFactory.build(InvalidMapper.class).getMethods()[0];

        MybatisExtException exception = assertThrows(MybatisExtException.class, () -> jpaParser.parse(tableInfo, method.getName(), method.getParameters(), method.getGenericReturnType()));

        assertTrue(exception.getMessage().contains("Invalid parameter type"));
        assertTrue(exception.getMessage().contains(TablePermission.class.getName()));
    }

    private Map<String, String> buildScripts() {
        GenericType genericType = GenericTypeFactory.build(JpaParserTestMapper.class);
        Map<String, Map<Semantic, String>> map = new HashMap<>();
        for (GenericMethod method : genericType.getMethods()) {
            Semantic semantic = jpaParser.parse(tableInfo, method.getName(), method.getParameters(), method.getGenericReturnType());
            ParameterSignature parameterSignature = ParameterSignatureHelper.buildParameterSignature(configuration, method);
            String s = ParameterSignatureHelper.toString(parameterSignature);
            map.computeIfAbsent(method.getName(), k -> new HashMap<>()).put(semantic, s);
        }
        Dialect dialect = new H2Dialect();
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> SemanticScriptHelper.buildScript(entry.getValue().entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)), dialect)));
    }

    interface InvalidMapper {
        int updatePermissionTypeByRoleId(@Param("permissionType") String permissionType, @Param("roleId") String roleId);
    }
}
