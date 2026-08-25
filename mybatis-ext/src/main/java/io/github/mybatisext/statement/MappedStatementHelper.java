package io.github.mybatisext.statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.JpaParser;
import io.github.mybatisext.jpa.Semantic;
import io.github.mybatisext.jpa.SemanticType;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;

public class MappedStatementHelper {

    private final JpaParser jpaParser;
    private final Configuration configuration;
    private final ExtContext extContext;
    private final ResultMapHelper resultMapHelper;

    public MappedStatementHelper(Configuration configuration, ExtContext extContext) {
        this.configuration = configuration;
        this.extContext = extContext;
        this.resultMapHelper = new ResultMapHelper(configuration, extContext);
        this.jpaParser = new JpaParser(configuration, extContext);
    }

    public MappedStatement build(String id, GenericType tableType, List<GenericMethod> methods, GenericType returnType) {
        TableInfo tableInfo = TableInfoFactory.buildTableInfo(tableType, configuration, extContext);
        Map<String, Semantic> signatureToSemantic = buildSignatureToSemantic(tableInfo, methods, returnType);
        SqlCommandType sqlCommandType = resolveSqlCommandType(signatureToSemantic.values().iterator().next());
        List<ResultMap> resultMaps = new ArrayList<>();
        resultMaps.add(resultMapHelper.buildResultMap(returnType));
        SqlSource sqlSource = new LazySqlSource(configuration, extContext, id, dialect -> SemanticScriptHelper.buildScript(signatureToSemantic, dialect));
        Builder builder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType);
        return builder.resultMaps(resultMaps).resultSetType(ResultSetType.DEFAULT).build();
    }

    private Map<String, Semantic> buildSignatureToSemantic(TableInfo tableInfo, List<GenericMethod> methods, GenericType returnType) {
        Map<Semantic, String> map = new HashMap<>();
        for (GenericMethod method : methods) {
            Semantic semantic = jpaParser.parse(tableInfo, method.getName(), method.getParameters(), returnType);
            map.put(semantic, buildParameterSignature(method));
        }
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    private String buildParameterSignature(GenericMethod method) {
        ParameterSignature parameterSignature = ParameterSignatureHelper.buildParameterSignature(configuration, method);
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
}
