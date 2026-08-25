package io.github.mybatisext.statement;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;

public class LazySqlSource implements SqlSource {
    private static final Log log = LogFactory.getLog(LazySqlSource.class);

    private final Configuration configuration;
    private final ExtContext extContext;
    private final String statementId;
    private final Function<Dialect, String> scriptFunction;
    // 按方言缓存，动态数据源场景下不同真实数据源各自初始化，避免首执行方言被冻结
    private final Map<Dialect, SqlSource> sqlSources = new ConcurrentHashMap<>();

    public LazySqlSource(Configuration configuration, ExtContext extContext, String statementId, Function<Dialect, String> scriptFunction) {
        this.configuration = configuration;
        this.extContext = extContext;
        this.statementId = statementId;
        this.scriptFunction = scriptFunction;
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        return sqlSources.computeIfAbsent(selectDialect(), this::createSqlSource).getBoundSql(parameterObject);
    }

    private SqlSource createSqlSource(Dialect dialect) {
        String script = scriptFunction.apply(dialect);
        log.debug(statementId);
        log.debug(script);
        return new XMLLanguageDriver().createSqlSource(configuration, script, Object.class);
    }

    // 执行期探测：动态数据源下此处获取的连接才指向真实数据源
    private Dialect selectDialect() {
        try (Connection connection = configuration.getEnvironment().getDataSource().getConnection()) {
            return extContext.getDialectSelector().select(connection.getMetaData().getURL());
        } catch (Exception e) {
            throw new MybatisExtException(e);
        }
    }
}
