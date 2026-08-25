package io.github.mybatisext.statement;

import java.util.function.Supplier;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

public class LazySqlSource implements SqlSource {
    private static final Log log = LogFactory.getLog(LazySqlSource.class);

    private final Configuration configuration;
    private final String statementId;
    private final Supplier<String> scriptSupplier;
    private SqlSource sqlSource;

    public LazySqlSource(Configuration configuration, String statementId, Supplier<String> scriptSupplier) {
        this.configuration = configuration;
        this.statementId = statementId;
        this.scriptSupplier = scriptSupplier;
        log.debug(statementId);
        log.debug(scriptSupplier.get());
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        if (sqlSource == null) {
            log.debug(statementId);
            String script = scriptSupplier.get();
            log.debug(script);
            sqlSource = new XMLLanguageDriver().createSqlSource(configuration, script, Object.class);
        }
        return sqlSource.getBoundSql(parameterObject);
    }

}
