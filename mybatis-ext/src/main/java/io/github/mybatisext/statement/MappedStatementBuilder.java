package io.github.mybatisext.statement;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

import io.github.mybatisext.ExtContext;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.JpaParser;
import io.github.mybatisext.jpa.Semantic;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.resultmap.ResultMapBuilder;

public class MappedStatementBuilder {

    private static final Log log = LogFactory.getLog(MappedStatementBuilder.class);
    private final Configuration originConfiguration;
    private final ExtContext extContext;
    private final JpaParser jpaParser = new JpaParser();

    public MappedStatementBuilder(Configuration originConfiguration, ExtContext extContext) {
        this.originConfiguration = originConfiguration;
        this.extContext = extContext;
    }

    public MappedStatement build(String id, GenericType tableType, List<GenericMethod> methods, GenericType returnType) {
        log.debug(id);
        TableInfo tableInfo = TableInfoFactory.getTableInfo(originConfiguration, tableType);
        Semantic semantic = getSemantic(tableInfo, methods);
        SemanticScriptBuilder semanticScriptBuilder = new SemanticScriptBuilder(selectDialect());
        String script = semanticScriptBuilder.buildScript(semantic);
        log.debug(script);
        SqlCommandType sqlCommandType = SqlCommandTypeResolver.resolve(semantic);
        List<ResultMap> resultMaps = getResultMaps(id, tableInfo, tableType, returnType);
        SqlSource sqlSource = new XMLLanguageDriver().createSqlSource(originConfiguration, script, Object.class);
        Builder builder = new MappedStatement.Builder(originConfiguration, id, sqlSource, sqlCommandType);
        return builder.resultMaps(resultMaps).resultSetType(ResultSetType.DEFAULT).build();
    }

    private Semantic getSemantic(TableInfo tableInfo, List<GenericMethod> methods) {
        Semantic semantic = null;
        for (GenericMethod method : methods) {
            Semantic sem = jpaParser.parse(originConfiguration, tableInfo, method.getName(), method.getParameters());
            if (semantic == null) {
                semantic = sem;
            } else if (!Objects.equals(semantic, sem)) {
                throw new MybatisExtException("Inconsistent semantics for method: " + method.getName());
            }
        }
        return semantic;
    }

    private List<ResultMap> getResultMaps(String id, TableInfo tableInfo, GenericType tableType, GenericType returnType) {
        List<ResultMap> resultMaps = new ArrayList<>();
        if (tableType.isAssignableFrom(returnType)) {
            resultMaps.add(ResultMapBuilder.buildResultMap(originConfiguration, tableInfo));
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
