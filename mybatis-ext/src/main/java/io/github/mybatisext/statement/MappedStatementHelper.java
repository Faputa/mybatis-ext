package io.github.mybatisext.statement;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.util.TypeUtils;

public class MappedStatementHelper {

    private static final Log log = LogFactory.getLog(MappedStatementHelper.class);
    private final Map<String, Map<String, Semantic>> semanticCache = new ConcurrentHashMap<>();
    private final JpaParser jpaParser;
    private final Configuration configuration;
    private final ExtContext extContext;
    private final ResultMapHelper resultMapHelper;
    private final TableInfoFactory tableInfoFactory;

    public MappedStatementHelper(Configuration configuration, ExtContext extContext, TableInfoFactory tableInfoFactory) {
        this.configuration = configuration;
        this.tableInfoFactory = tableInfoFactory;
        this.extContext = extContext;
        this.resultMapHelper = new ResultMapHelper(configuration, tableInfoFactory);
        this.jpaParser = new JpaParser(configuration, tableInfoFactory);
    }

    public MappedStatement build(String id, GenericType tableType, List<GenericMethod> methods, GenericType returnType, boolean writeConfiguration) {
        log.debug(id);
        Map<String, Semantic> signatureToSemantic = semanticCache.computeIfAbsent(id,
                key -> buildSignatureToSemantic(key, tableInfoFactory.buildTableInfo(tableType), methods, returnType));
        Dialect dialect = selectDialect();
        String script = SemanticScriptHelper.buildScript(signatureToSemantic, dialect);
        log.debug(script);
        SqlCommandType sqlCommandType = resolveSqlCommandType(signatureToSemantic.values().iterator().next());
        List<ResultMap> resultMaps = new ArrayList<>();
        resultMaps.add(resultMapHelper.buildResultMap(returnType, dialect, writeConfiguration));
        SqlSource sqlSource = new XMLLanguageDriver().createSqlSource(configuration, script, Object.class);
        Builder builder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType);
        return builder.resultMaps(resultMaps).resultSetType(ResultSetType.DEFAULT).build();
    }

    private Map<String, Semantic> buildSignatureToSemantic(String id, TableInfo tableInfo, List<GenericMethod> methods, GenericType returnType) {
        Map<String, Semantic> signatureToSemantic = new HashMap<>();
        for (GenericMethod method : methods) {
            Semantic semantic = jpaParser.parse(tableInfo, method, returnType);
            String signature = buildParameterSignature(method);
            Semantic previous = signatureToSemantic.putIfAbsent(signature, semantic);
            if (previous != null && !previous.equals(semantic)) {
                throw new MybatisExtException("Conflicting overload semantics for " + id + ", normalized parameter signature: " + signature);
            }
        }
        return Collections.unmodifiableMap(signatureToSemantic);
    }

    private String buildParameterSignature(GenericMethod method) {
        ParameterSignature parameterSignature = ParameterSignatureHelper.buildParameterSignature(configuration, method);
        if (parameterSignature.getType() != null) {
            parameterSignature.setType(TypeUtils.wrapPrimitive(parameterSignature.getType()));
        }
        parameterSignature.getNameToType().replaceAll((name, type) -> TypeUtils.wrapPrimitive(type));
        return ParameterSignatureHelper.toString(parameterSignature);
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

    private Dialect selectDialect() {
        DataSource dataSource = configuration.getEnvironment().getDataSource();
        String jdbcUrl;
        try (Connection connection = dataSource.getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
        } catch (Exception e) {
            throw new MybatisExtException(e);
        }
        return extContext.getDialectSelector().select(jdbcUrl);
    }
}
