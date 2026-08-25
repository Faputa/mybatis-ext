package io.github.mybatisext.statement;

import java.sql.Connection;
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
    private volatile SqlSource sqlSource;

    public LazySqlSource(Configuration configuration, ExtContext extContext, String statementId, Function<Dialect, String> scriptFunction) {
        this.configuration = configuration;
        this.extContext = extContext;
        this.statementId = statementId;
        this.scriptFunction = scriptFunction;
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        SqlSource source = sqlSource;
        if (source == null) {
            source = createSqlSource();
            sqlSource = source;
        }
        return source.getBoundSql(parameterObject);
    }

    private SqlSource createSqlSource() {
        Dialect dialect = selectDialect();
        String script = scriptFunction.apply(dialect);
        log.debug(statementId);
        log.debug(script);
        return new XMLLanguageDriver().createSqlSource(configuration, script, Object.class);
    }

    // 执行期探测：动态数据源下此处获取的连接才指向真实数据源。
    // 假设单个Configuration服务的数据库类型固定（含同类型动态数据源），方言首次确定后不再变化
    private Dialect selectDialect() {
        try (Connection connection = configuration.getEnvironment().getDataSource().getConnection()) {
            return extContext.getDialectSelector().select(connection.getMetaData().getURL());
        } catch (Exception e) {
            throw new MybatisExtException(e);
        }
    }
}
