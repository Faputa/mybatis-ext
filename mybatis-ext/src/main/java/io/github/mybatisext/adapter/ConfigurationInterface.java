package io.github.mybatisext.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.builder.annotation.MethodResolver;
import org.apache.ibatis.builder.xml.XMLStatementBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.LanguageDriverRegistry;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * 此接口的方法应与 {@link org.apache.ibatis.session.Configuration} 中所有的public方法保持同步
 */
public interface ConfigurationInterface {

    String getLogPrefix();

    void setLogPrefix(String logPrefix);

    Class<? extends Log> getLogImpl();

    void setLogImpl(Class<? extends Log> logImpl);

    Class<? extends VFS> getVfsImpl();

    void setVfsImpl(Class<? extends VFS> vfsImpl);

    Class<?> getDefaultSqlProviderType();

    void setDefaultSqlProviderType(Class<?> defaultSqlProviderType);

    boolean isCallSettersOnNulls();

    void setCallSettersOnNulls(boolean callSettersOnNulls);

    boolean isUseActualParamName();

    void setUseActualParamName(boolean useActualParamName);

    boolean isReturnInstanceForEmptyRow();

    void setReturnInstanceForEmptyRow(boolean returnEmptyInstance);

    boolean isShrinkWhitespacesInSql();

    void setShrinkWhitespacesInSql(boolean shrinkWhitespacesInSql);

    void setNullableOnForEach(boolean nullableOnForEach);

    boolean isNullableOnForEach();

    boolean isArgNameBasedConstructorAutoMapping();

    void setArgNameBasedConstructorAutoMapping(boolean argNameBasedConstructorAutoMapping);

    String getDatabaseId();

    void setDatabaseId(String databaseId);

    Class<?> getConfigurationFactory();

    void setConfigurationFactory(Class<?> configurationFactory);

    boolean isSafeResultHandlerEnabled();

    void setSafeResultHandlerEnabled(boolean safeResultHandlerEnabled);

    boolean isSafeRowBoundsEnabled();

    void setSafeRowBoundsEnabled(boolean safeRowBoundsEnabled);

    boolean isMapUnderscoreToCamelCase();

    void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase);

    void addLoadedResource(String resource);

    boolean isResourceLoaded(String resource);

    Environment getEnvironment();

    void setEnvironment(Environment environment);

    AutoMappingBehavior getAutoMappingBehavior();

    void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior);

    AutoMappingUnknownColumnBehavior getAutoMappingUnknownColumnBehavior();

    void setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior);

    boolean isLazyLoadingEnabled();

    void setLazyLoadingEnabled(boolean lazyLoadingEnabled);

    ProxyFactory getProxyFactory();

    void setProxyFactory(ProxyFactory proxyFactory);

    boolean isAggressiveLazyLoading();

    void setAggressiveLazyLoading(boolean aggressiveLazyLoading);

    boolean isMultipleResultSetsEnabled();

    void setMultipleResultSetsEnabled(boolean multipleResultSetsEnabled);

    Set<String> getLazyLoadTriggerMethods();

    void setLazyLoadTriggerMethods(Set<String> lazyLoadTriggerMethods);

    boolean isUseGeneratedKeys();

    void setUseGeneratedKeys(boolean useGeneratedKeys);

    ExecutorType getDefaultExecutorType();

    void setDefaultExecutorType(ExecutorType defaultExecutorType);

    boolean isCacheEnabled();

    void setCacheEnabled(boolean cacheEnabled);

    Integer getDefaultStatementTimeout();

    void setDefaultStatementTimeout(Integer defaultStatementTimeout);

    Integer getDefaultFetchSize();

    void setDefaultFetchSize(Integer defaultFetchSize);

    ResultSetType getDefaultResultSetType();

    void setDefaultResultSetType(ResultSetType defaultResultSetType);

    boolean isUseColumnLabel();

    void setUseColumnLabel(boolean useColumnLabel);

    LocalCacheScope getLocalCacheScope();

    void setLocalCacheScope(LocalCacheScope localCacheScope);

    JdbcType getJdbcTypeForNull();

    void setJdbcTypeForNull(JdbcType jdbcTypeForNull);

    Properties getVariables();

    void setVariables(Properties variables);

    TypeHandlerRegistry getTypeHandlerRegistry();

    @SuppressWarnings("rawtypes")
    void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler);

    TypeAliasRegistry getTypeAliasRegistry();

    MapperRegistry getMapperRegistry();

    ReflectorFactory getReflectorFactory();

    void setReflectorFactory(ReflectorFactory reflectorFactory);

    ObjectFactory getObjectFactory();

    void setObjectFactory(ObjectFactory objectFactory);

    ObjectWrapperFactory getObjectWrapperFactory();

    void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory);

    List<Interceptor> getInterceptors();

    LanguageDriverRegistry getLanguageRegistry();

    void setDefaultScriptingLanguage(Class<? extends LanguageDriver> driver);

    LanguageDriver getDefaultScriptingLanguageInstance();

    LanguageDriver getLanguageDriver(Class<? extends LanguageDriver> langClass);

    LanguageDriver getDefaultScriptingLanuageInstance();

    MetaObject newMetaObject(Object object);

    ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);

    @SuppressWarnings("rawtypes")
    ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler, ResultHandler resultHandler, BoundSql boundSql);

    @SuppressWarnings("rawtypes")
    StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql);

    Executor newExecutor(Transaction transaction);

    Executor newExecutor(Transaction transaction, ExecutorType executorType);

    void addKeyGenerator(String id, KeyGenerator keyGenerator);

    Collection<String> getKeyGeneratorNames();

    Collection<KeyGenerator> getKeyGenerators();

    KeyGenerator getKeyGenerator(String id);

    boolean hasKeyGenerator(String id);

    void addCache(Cache cache);

    Collection<String> getCacheNames();

    Collection<Cache> getCaches();

    Cache getCache(String id);

    boolean hasCache(String id);

    void addResultMap(ResultMap rm);

    Collection<String> getResultMapNames();

    Collection<ResultMap> getResultMaps();

    ResultMap getResultMap(String id);

    boolean hasResultMap(String id);

    void addParameterMap(ParameterMap pm);

    Collection<String> getParameterMapNames();

    Collection<ParameterMap> getParameterMaps();

    ParameterMap getParameterMap(String id);

    boolean hasParameterMap(String id);

    void addMappedStatement(MappedStatement ms);

    Collection<String> getMappedStatementNames();

    Collection<MappedStatement> getMappedStatements();

    Collection<XMLStatementBuilder> getIncompleteStatements();

    void addIncompleteStatement(XMLStatementBuilder incompleteStatement);

    Collection<CacheRefResolver> getIncompleteCacheRefs();

    void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef);

    Collection<ResultMapResolver> getIncompleteResultMaps();

    void addIncompleteResultMap(ResultMapResolver resultMapResolver);

    void addIncompleteMethod(MethodResolver builder);

    Collection<MethodResolver> getIncompleteMethods();

    MappedStatement getMappedStatement(String id);

    MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements);

    Map<String, XNode> getSqlFragments();

    void addInterceptor(Interceptor interceptor);

    void addMappers(String packageName, Class<?> superType);

    void addMappers(String packageName);

    <T> void addMapper(Class<T> type);

    <T> T getMapper(Class<T> type, SqlSession sqlSession);

    boolean hasMapper(Class<?> type);

    boolean hasStatement(String statementName);

    boolean hasStatement(String statementName, boolean validateIncompleteStatements);

    void addCacheRef(String namespace, String referencedNamespace);

    void validateAllMapperMethod();
}
