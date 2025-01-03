package io.github.mybatisext.statement;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.JpaParser;
import io.github.mybatisext.jpa.Semantic;
import io.github.mybatisext.jpa.SemanticType;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericParameter;
import io.github.mybatisext.reflect.GenericType;

public class MappedStatementHelper {

    private static final Log log = LogFactory.getLog(MappedStatementHelper.class);
    private final Configuration originConfiguration;
    private final ExtContext extContext;
    private final JpaParser jpaParser = new JpaParser();

    public MappedStatementHelper(Configuration originConfiguration, ExtContext extContext) {
        this.originConfiguration = originConfiguration;
        this.extContext = extContext;
    }

    public MappedStatement build(String id, GenericType tableType, List<GenericMethod> methods, GenericType returnType) {
        log.debug(id);
        TableInfo tableInfo = TableInfoFactory.getTableInfo(originConfiguration, tableType);
        Map<String, Semantic> signatureToSemantic = buildSignatureToSemantic(tableInfo, methods);
        String script = buildScript(signatureToSemantic);
        log.debug(script);
        SqlCommandType sqlCommandType = resolveSqlCommandType(signatureToSemantic.values().iterator().next());
        List<ResultMap> resultMaps = getResultMaps(id, tableInfo, tableType, returnType);
        SqlSource sqlSource = new XMLLanguageDriver().createSqlSource(originConfiguration, script, Object.class);
        Builder builder = new MappedStatement.Builder(originConfiguration, id, sqlSource, sqlCommandType);
        return builder.resultMaps(resultMaps).resultSetType(ResultSetType.DEFAULT).build();
    }

    private Map<String, Semantic> buildSignatureToSemantic(TableInfo tableInfo, List<GenericMethod> methods) {
        Map<String, Semantic> map = new HashMap<>();
        for (GenericMethod method : methods) {
            Semantic semantic = jpaParser.parse(originConfiguration, tableInfo, method.getName(), method.getParameters());
            String signature = buildParameterSignature(method.getParameters());
            map.put(signature, semantic);
        }
        return map;
    }

    private String buildParameterSignature(GenericParameter[] parameters) {
        // TODO
        return "";
    }

    private String buildScript(Map<String, Semantic> signatureToSemantic) {
        return SemanticScriptHelper.buildScript(signatureToSemantic, selectDialect());
    }

    private SqlCommandType resolveSqlCommandType(Semantic semantic) {
        if (semantic.getType() == SemanticType.COUNT || semantic.getType() == SemanticType.EXISTS || semantic.getType() == SemanticType.SELECT) {
            return SqlCommandType.SELECT;
        }
        if (semantic.getType() == SemanticType.DELETE) {
            return SqlCommandType.DELETE;
        }
        if (semantic.getType() == SemanticType.INSERT) {
            return SqlCommandType.INSERT;
        }
        if (semantic.getType() == SemanticType.UPDATE) {
            return SqlCommandType.UPDATE;
        }
        throw new MybatisExtException("Unsupported semantic type: " + semantic.getType());
    }

    private List<ResultMap> getResultMaps(String id, TableInfo tableInfo, GenericType tableType, GenericType returnType) {
        List<ResultMap> resultMaps = new ArrayList<>();
        if (tableType.isAssignableFrom(returnType)) {
            resultMaps.add(ResultMapHelper.buildResultMap(originConfiguration, tableInfo));
        } else {
            resultMaps.add(new ResultMap.Builder(originConfiguration, id + "-Inline", returnType.getType(), new ArrayList<>(0)).build());
        }
        return resultMaps;
    }

    private Dialect selectDialect() {
        DataSource dataSource = originConfiguration.getEnvironment().getDataSource();
        String jdbcUrl;
        try (Connection connection = dataSource.getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
        } catch (Exception e) {
            throw new MybatisExtException(e);
        }
        return extContext.getDialectSelector().select(jdbcUrl);
    }
}
