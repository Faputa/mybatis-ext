package io.github.mybatisext;

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

    public String getLogPrefix();

    public void setLogPrefix(String logPrefix);

    public Class<? extends Log> getLogImpl();

    public void setLogImpl(Class<? extends Log> logImpl);

    public Class<? extends VFS> getVfsImpl();

    public void setVfsImpl(Class<? extends VFS> vfsImpl);

    public Class<?> getDefaultSqlProviderType();

    public void setDefaultSqlProviderType(Class<?> defaultSqlProviderType);

    public boolean isCallSettersOnNulls();

    public void setCallSettersOnNulls(boolean callSettersOnNulls);

    public boolean isUseActualParamName();

    public void setUseActualParamName(boolean useActualParamName);

    public boolean isReturnInstanceForEmptyRow();

    public void setReturnInstanceForEmptyRow(boolean returnEmptyInstance);

    public boolean isShrinkWhitespacesInSql();

    public void setShrinkWhitespacesInSql(boolean shrinkWhitespacesInSql);

    public void setNullableOnForEach(boolean nullableOnForEach);

    public boolean isNullableOnForEach();

    public boolean isArgNameBasedConstructorAutoMapping();

    public void setArgNameBasedConstructorAutoMapping(boolean argNameBasedConstructorAutoMapping);

    public String getDatabaseId();

    public void setDatabaseId(String databaseId);

    public Class<?> getConfigurationFactory();

    public void setConfigurationFactory(Class<?> configurationFactory);

    public boolean isSafeResultHandlerEnabled();

    public void setSafeResultHandlerEnabled(boolean safeResultHandlerEnabled);

    public boolean isSafeRowBoundsEnabled();

    public void setSafeRowBoundsEnabled(boolean safeRowBoundsEnabled);

    public boolean isMapUnderscoreToCamelCase();

    public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase);

    public void addLoadedResource(String resource);

    public boolean isResourceLoaded(String resource);

    public Environment getEnvironment();

    public void setEnvironment(Environment environment);

    public AutoMappingBehavior getAutoMappingBehavior();

    public void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior);

    public AutoMappingUnknownColumnBehavior getAutoMappingUnknownColumnBehavior();

    public void setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior);

    public boolean isLazyLoadingEnabled();

    public void setLazyLoadingEnabled(boolean lazyLoadingEnabled);

    public ProxyFactory getProxyFactory();

    public void setProxyFactory(ProxyFactory proxyFactory);

    public boolean isAggressiveLazyLoading();

    public void setAggressiveLazyLoading(boolean aggressiveLazyLoading);

    public boolean isMultipleResultSetsEnabled();

    public void setMultipleResultSetsEnabled(boolean multipleResultSetsEnabled);

    public Set<String> getLazyLoadTriggerMethods();

    public void setLazyLoadTriggerMethods(Set<String> lazyLoadTriggerMethods);

    public boolean isUseGeneratedKeys();

    public void setUseGeneratedKeys(boolean useGeneratedKeys);

    public ExecutorType getDefaultExecutorType();

    public void setDefaultExecutorType(ExecutorType defaultExecutorType);

    public boolean isCacheEnabled();

    public void setCacheEnabled(boolean cacheEnabled);

    public Integer getDefaultStatementTimeout();

    public void setDefaultStatementTimeout(Integer defaultStatementTimeout);

    public Integer getDefaultFetchSize();

    public void setDefaultFetchSize(Integer defaultFetchSize);

    public ResultSetType getDefaultResultSetType();

    public void setDefaultResultSetType(ResultSetType defaultResultSetType);

    public boolean isUseColumnLabel();

    public void setUseColumnLabel(boolean useColumnLabel);

    public LocalCacheScope getLocalCacheScope();

    public void setLocalCacheScope(LocalCacheScope localCacheScope);

    public JdbcType getJdbcTypeForNull();

    public void setJdbcTypeForNull(JdbcType jdbcTypeForNull);

    public Properties getVariables();

    public void setVariables(Properties variables);

    public TypeHandlerRegistry getTypeHandlerRegistry();

    @SuppressWarnings("rawtypes")
    public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler);

    public TypeAliasRegistry getTypeAliasRegistry();

    public MapperRegistry getMapperRegistry();

    public ReflectorFactory getReflectorFactory();

    public void setReflectorFactory(ReflectorFactory reflectorFactory);

    public ObjectFactory getObjectFactory();

    public void setObjectFactory(ObjectFactory objectFactory);

    public ObjectWrapperFactory getObjectWrapperFactory();

    public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory);

    public List<Interceptor> getInterceptors();

    public LanguageDriverRegistry getLanguageRegistry();

    public void setDefaultScriptingLanguage(Class<? extends LanguageDriver> driver);

    public LanguageDriver getDefaultScriptingLanguageInstance();

    public LanguageDriver getLanguageDriver(Class<? extends LanguageDriver> langClass);

    public LanguageDriver getDefaultScriptingLanuageInstance();

    public MetaObject newMetaObject(Object object);

    public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);

    @SuppressWarnings("rawtypes")
    public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler, ResultHandler resultHandler, BoundSql boundSql);

    @SuppressWarnings("rawtypes")
    public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql);

    public Executor newExecutor(Transaction transaction);

    public Executor newExecutor(Transaction transaction, ExecutorType executorType);

    public void addKeyGenerator(String id, KeyGenerator keyGenerator);

    public Collection<String> getKeyGeneratorNames();

    public Collection<KeyGenerator> getKeyGenerators();

    public KeyGenerator getKeyGenerator(String id);

    public boolean hasKeyGenerator(String id);

    public void addCache(Cache cache);

    public Collection<String> getCacheNames();

    public Collection<Cache> getCaches();

    public Cache getCache(String id);

    public boolean hasCache(String id);

    public void addResultMap(ResultMap rm);

    public Collection<String> getResultMapNames();

    public Collection<ResultMap> getResultMaps();

    public ResultMap getResultMap(String id);

    public boolean hasResultMap(String id);

    public void addParameterMap(ParameterMap pm);

    public Collection<String> getParameterMapNames();

    public Collection<ParameterMap> getParameterMaps();

    public ParameterMap getParameterMap(String id);

    public boolean hasParameterMap(String id);

    public void addMappedStatement(MappedStatement ms);

    public Collection<String> getMappedStatementNames();

    public Collection<MappedStatement> getMappedStatements();

    public Collection<XMLStatementBuilder> getIncompleteStatements();

    public void addIncompleteStatement(XMLStatementBuilder incompleteStatement);

    public Collection<CacheRefResolver> getIncompleteCacheRefs();

    public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef);

    public Collection<ResultMapResolver> getIncompleteResultMaps();

    public void addIncompleteResultMap(ResultMapResolver resultMapResolver);

    public void addIncompleteMethod(MethodResolver builder);

    public Collection<MethodResolver> getIncompleteMethods();

    public MappedStatement getMappedStatement(String id);

    public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements);

    public Map<String, XNode> getSqlFragments();

    public void addInterceptor(Interceptor interceptor);

    public void addMappers(String packageName, Class<?> superType);

    public void addMappers(String packageName);

    public <T> void addMapper(Class<T> type);

    public <T> T getMapper(Class<T> type, SqlSession sqlSession);

    public boolean hasMapper(Class<?> type);

    public boolean hasStatement(String statementName);

    public boolean hasStatement(String statementName, boolean validateIncompleteStatements);

    public void addCacheRef(String namespace, String referencedNamespace);
}
