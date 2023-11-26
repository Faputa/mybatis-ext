package com.mybatisext;

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
import org.apache.ibatis.session.Configuration;
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

public class ExtConfiguration extends Configuration implements ConfigurationInterface {

    private Configuration originConfiguration;
    private ExtEnhancer extEnhancer;

    public ExtConfiguration() {
        this.originConfiguration = new Configuration();
        this.extEnhancer = new ExtEnhancer(originConfiguration);
    }

    public ExtConfiguration(Environment environment) {
        this.originConfiguration = new Configuration(environment);
        this.extEnhancer = new ExtEnhancer(originConfiguration);
    }

    public ExtConfiguration(Configuration configuration) {
        this.originConfiguration = configuration;
        this.extEnhancer = new ExtEnhancer(originConfiguration);
    }

    @Override
    public String getLogPrefix() {
        return this.originConfiguration.getLogPrefix();
    }

    @Override
    public void setLogPrefix(String logPrefix) {
        this.originConfiguration.setLogPrefix(logPrefix);
    }

    @Override
    public Class<? extends Log> getLogImpl() {
        return this.originConfiguration.getLogImpl();
    }

    @Override
    public void setLogImpl(Class<? extends Log> logImpl) {
        this.originConfiguration.setLogImpl(logImpl);
    }

    @Override
    public Class<? extends VFS> getVfsImpl() {
        return this.originConfiguration.getVfsImpl();
    }

    @Override
    public void setVfsImpl(Class<? extends VFS> vfsImpl) {
        this.originConfiguration.setVfsImpl(vfsImpl);
    }

    @Override
    public Class<?> getDefaultSqlProviderType() {
        return this.originConfiguration.getDefaultSqlProviderType();
    }

    @Override
    public void setDefaultSqlProviderType(Class<?> defaultSqlProviderType) {
        this.originConfiguration.setDefaultSqlProviderType(defaultSqlProviderType);
    }

    @Override
    public boolean isCallSettersOnNulls() {
        return this.originConfiguration.isCallSettersOnNulls();
    }

    @Override
    public void setCallSettersOnNulls(boolean callSettersOnNulls) {
        this.originConfiguration.setCallSettersOnNulls(callSettersOnNulls);
    }

    @Override
    public boolean isUseActualParamName() {
        return this.originConfiguration.isUseActualParamName();
    }

    @Override
    public void setUseActualParamName(boolean useActualParamName) {
        this.originConfiguration.setUseActualParamName(useActualParamName);
    }

    @Override
    public boolean isReturnInstanceForEmptyRow() {
        return this.originConfiguration.isReturnInstanceForEmptyRow();
    }

    @Override
    public void setReturnInstanceForEmptyRow(boolean returnEmptyInstance) {
        this.originConfiguration.setReturnInstanceForEmptyRow(returnEmptyInstance);
    }

    @Override
    public boolean isShrinkWhitespacesInSql() {
        return this.originConfiguration.isShrinkWhitespacesInSql();
    }

    @Override
    public void setShrinkWhitespacesInSql(boolean shrinkWhitespacesInSql) {
        this.originConfiguration.setShrinkWhitespacesInSql(shrinkWhitespacesInSql);
    }

    @Override
    public void setNullableOnForEach(boolean nullableOnForEach) {
        this.originConfiguration.setNullableOnForEach(nullableOnForEach);
    }

    @Override
    public boolean isNullableOnForEach() {
        return this.originConfiguration.isNullableOnForEach();
    }

    @Override
    public boolean isArgNameBasedConstructorAutoMapping() {
        return this.originConfiguration.isArgNameBasedConstructorAutoMapping();
    }

    @Override
    public void setArgNameBasedConstructorAutoMapping(boolean argNameBasedConstructorAutoMapping) {
        this.originConfiguration.setArgNameBasedConstructorAutoMapping(argNameBasedConstructorAutoMapping);
    }

    @Override
    public String getDatabaseId() {
        return this.originConfiguration.getDatabaseId();
    }

    @Override
    public void setDatabaseId(String databaseId) {
        this.originConfiguration.setDatabaseId(databaseId);
    }

    @Override
    public Class<?> getConfigurationFactory() {
        return this.originConfiguration.getConfigurationFactory();
    }

    @Override
    public void setConfigurationFactory(Class<?> configurationFactory) {
        this.originConfiguration.setConfigurationFactory(configurationFactory);
    }

    @Override
    public boolean isSafeResultHandlerEnabled() {
        return this.originConfiguration.isSafeResultHandlerEnabled();
    }

    @Override
    public void setSafeResultHandlerEnabled(boolean safeResultHandlerEnabled) {
        this.originConfiguration.setSafeResultHandlerEnabled(safeResultHandlerEnabled);
    }

    @Override
    public boolean isSafeRowBoundsEnabled() {
        return this.originConfiguration.isSafeRowBoundsEnabled();
    }

    @Override
    public void setSafeRowBoundsEnabled(boolean safeRowBoundsEnabled) {
        this.originConfiguration.setSafeRowBoundsEnabled(safeRowBoundsEnabled);
    }

    @Override
    public boolean isMapUnderscoreToCamelCase() {
        return this.originConfiguration.isMapUnderscoreToCamelCase();
    }

    @Override
    public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
        this.originConfiguration.setMapUnderscoreToCamelCase(mapUnderscoreToCamelCase);
    }

    @Override
    public void addLoadedResource(String resource) {
        this.originConfiguration.addLoadedResource(resource);
    }

    @Override
    public boolean isResourceLoaded(String resource) {
        return this.originConfiguration.isResourceLoaded(resource);
    }

    @Override
    public Environment getEnvironment() {
        return this.originConfiguration.getEnvironment();
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.originConfiguration.setEnvironment(environment);
    }

    @Override
    public AutoMappingBehavior getAutoMappingBehavior() {
        return this.originConfiguration.getAutoMappingBehavior();
    }

    @Override
    public void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior) {
        this.originConfiguration.setAutoMappingBehavior(autoMappingBehavior);
    }

    @Override
    public AutoMappingUnknownColumnBehavior getAutoMappingUnknownColumnBehavior() {
        return this.originConfiguration.getAutoMappingUnknownColumnBehavior();
    }

    @Override
    public void setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior) {
        this.originConfiguration.setAutoMappingUnknownColumnBehavior(autoMappingUnknownColumnBehavior);
    }

    @Override
    public boolean isLazyLoadingEnabled() {
        return this.originConfiguration.isLazyLoadingEnabled();
    }

    @Override
    public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {
        this.originConfiguration.setLazyLoadingEnabled(lazyLoadingEnabled);
    }

    @Override
    public ProxyFactory getProxyFactory() {
        return this.originConfiguration.getProxyFactory();
    }

    @Override
    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.originConfiguration.setProxyFactory(proxyFactory);
    }

    @Override
    public boolean isAggressiveLazyLoading() {
        return this.originConfiguration.isAggressiveLazyLoading();
    }

    @Override
    public void setAggressiveLazyLoading(boolean aggressiveLazyLoading) {
        this.originConfiguration.setAggressiveLazyLoading(aggressiveLazyLoading);
    }

    @Override
    public boolean isMultipleResultSetsEnabled() {
        return this.originConfiguration.isMultipleResultSetsEnabled();
    }

    @Override
    public void setMultipleResultSetsEnabled(boolean multipleResultSetsEnabled) {
        this.originConfiguration.setMultipleResultSetsEnabled(multipleResultSetsEnabled);
    }

    @Override
    public Set<String> getLazyLoadTriggerMethods() {
        return this.originConfiguration.getLazyLoadTriggerMethods();
    }

    @Override
    public void setLazyLoadTriggerMethods(Set<String> lazyLoadTriggerMethods) {
        this.originConfiguration.setLazyLoadTriggerMethods(lazyLoadTriggerMethods);
    }

    @Override
    public boolean isUseGeneratedKeys() {
        return this.originConfiguration.isUseGeneratedKeys();
    }

    @Override
    public void setUseGeneratedKeys(boolean useGeneratedKeys) {
        this.originConfiguration.setUseGeneratedKeys(useGeneratedKeys);
    }

    @Override
    public ExecutorType getDefaultExecutorType() {
        return this.originConfiguration.getDefaultExecutorType();
    }

    @Override
    public void setDefaultExecutorType(ExecutorType defaultExecutorType) {
        this.originConfiguration.setDefaultExecutorType(defaultExecutorType);
    }

    @Override
    public boolean isCacheEnabled() {
        return this.originConfiguration.isCacheEnabled();
    }

    @Override
    public void setCacheEnabled(boolean cacheEnabled) {
        this.originConfiguration.setCacheEnabled(cacheEnabled);
    }

    @Override
    public Integer getDefaultStatementTimeout() {
        return this.originConfiguration.getDefaultStatementTimeout();
    }

    @Override
    public void setDefaultStatementTimeout(Integer defaultStatementTimeout) {
        this.originConfiguration.setDefaultStatementTimeout(defaultStatementTimeout);
    }

    @Override
    public Integer getDefaultFetchSize() {
        return this.originConfiguration.getDefaultFetchSize();
    }

    @Override
    public void setDefaultFetchSize(Integer defaultFetchSize) {
        this.originConfiguration.setDefaultFetchSize(defaultFetchSize);
    }

    @Override
    public ResultSetType getDefaultResultSetType() {
        return this.originConfiguration.getDefaultResultSetType();
    }

    @Override
    public void setDefaultResultSetType(ResultSetType defaultResultSetType) {
        this.originConfiguration.setDefaultResultSetType(defaultResultSetType);
    }

    @Override
    public boolean isUseColumnLabel() {
        return this.originConfiguration.isUseColumnLabel();
    }

    @Override
    public void setUseColumnLabel(boolean useColumnLabel) {
        this.originConfiguration.setUseColumnLabel(useColumnLabel);
    }

    @Override
    public LocalCacheScope getLocalCacheScope() {
        return this.originConfiguration.getLocalCacheScope();
    }

    @Override
    public void setLocalCacheScope(LocalCacheScope localCacheScope) {
        this.originConfiguration.setLocalCacheScope(localCacheScope);
    }

    @Override
    public JdbcType getJdbcTypeForNull() {
        return this.originConfiguration.getJdbcTypeForNull();
    }

    @Override
    public void setJdbcTypeForNull(JdbcType jdbcTypeForNull) {
        this.originConfiguration.setJdbcTypeForNull(jdbcTypeForNull);
    }

    @Override
    public Properties getVariables() {
        return this.originConfiguration.getVariables();
    }

    @Override
    public void setVariables(Properties variables) {
        this.originConfiguration.setVariables(variables);
    }

    @Override
    public TypeHandlerRegistry getTypeHandlerRegistry() {
        return this.originConfiguration.getTypeHandlerRegistry();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
        this.originConfiguration.setDefaultEnumTypeHandler(typeHandler);
    }

    @Override
    public TypeAliasRegistry getTypeAliasRegistry() {
        return this.originConfiguration.getTypeAliasRegistry();
    }

    @Override
    public MapperRegistry getMapperRegistry() {
        return this.originConfiguration.getMapperRegistry();
    }

    @Override
    public ReflectorFactory getReflectorFactory() {
        return this.originConfiguration.getReflectorFactory();
    }

    @Override
    public void setReflectorFactory(ReflectorFactory reflectorFactory) {
        this.originConfiguration.setReflectorFactory(reflectorFactory);
    }

    @Override
    public ObjectFactory getObjectFactory() {
        return this.originConfiguration.getObjectFactory();
    }

    @Override
    public void setObjectFactory(ObjectFactory objectFactory) {
        this.originConfiguration.setObjectFactory(objectFactory);
    }

    @Override
    public ObjectWrapperFactory getObjectWrapperFactory() {
        return this.originConfiguration.getObjectWrapperFactory();
    }

    @Override
    public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
        this.originConfiguration.setObjectWrapperFactory(objectWrapperFactory);
    }

    @Override
    public List<Interceptor> getInterceptors() {
        return this.originConfiguration.getInterceptors();
    }

    @Override
    public LanguageDriverRegistry getLanguageRegistry() {
        return this.originConfiguration.getLanguageRegistry();
    }

    @Override
    public void setDefaultScriptingLanguage(Class<? extends LanguageDriver> driver) {
        this.originConfiguration.setDefaultScriptingLanguage(driver);
    }

    @Override
    public LanguageDriver getDefaultScriptingLanguageInstance() {
        return this.originConfiguration.getDefaultScriptingLanguageInstance();
    }

    @Override
    public LanguageDriver getLanguageDriver(Class<? extends LanguageDriver> langClass) {
        return this.originConfiguration.getLanguageDriver(langClass);
    }

    @Override
    @Deprecated
    public LanguageDriver getDefaultScriptingLanuageInstance() {
        return this.originConfiguration.getDefaultScriptingLanuageInstance();
    }

    @Override
    public MetaObject newMetaObject(Object object) {
        return this.originConfiguration.newMetaObject(object);
    }

    @Override
    public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
        return this.originConfiguration.newParameterHandler(mappedStatement, parameterObject, boundSql);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler, ResultHandler resultHandler, BoundSql boundSql) {
        return this.originConfiguration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        return this.originConfiguration.newStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
    }

    @Override
    public Executor newExecutor(Transaction transaction) {
        return this.originConfiguration.newExecutor(transaction);
    }

    @Override
    public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
        return this.originConfiguration.newExecutor(transaction, executorType);
    }

    @Override
    public void addKeyGenerator(String id, KeyGenerator keyGenerator) {
        this.originConfiguration.addKeyGenerator(id, keyGenerator);
    }

    @Override
    public Collection<String> getKeyGeneratorNames() {
        return this.originConfiguration.getKeyGeneratorNames();
    }

    @Override
    public Collection<KeyGenerator> getKeyGenerators() {
        return this.originConfiguration.getKeyGenerators();
    }

    @Override
    public KeyGenerator getKeyGenerator(String id) {
        return this.originConfiguration.getKeyGenerator(id);
    }

    @Override
    public boolean hasKeyGenerator(String id) {
        return this.originConfiguration.hasKeyGenerator(id);
    }

    @Override
    public void addCache(Cache cache) {
        this.originConfiguration.addCache(cache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return this.originConfiguration.getCacheNames();
    }

    @Override
    public Collection<Cache> getCaches() {
        return this.originConfiguration.getCaches();
    }

    @Override
    public Cache getCache(String id) {
        return this.originConfiguration.getCache(id);
    }

    @Override
    public boolean hasCache(String id) {
        return this.originConfiguration.hasCache(id);
    }

    @Override
    public void addResultMap(ResultMap rm) {
        this.originConfiguration.addResultMap(rm);
    }

    @Override
    public Collection<String> getResultMapNames() {
        return this.originConfiguration.getResultMapNames();
    }

    @Override
    public Collection<ResultMap> getResultMaps() {
        return this.originConfiguration.getResultMaps();
    }

    @Override
    public ResultMap getResultMap(String id) {
        return this.originConfiguration.getResultMap(id);
    }

    @Override
    public boolean hasResultMap(String id) {
        return this.originConfiguration.hasResultMap(id);
    }

    @Override
    public void addParameterMap(ParameterMap pm) {
        this.originConfiguration.addParameterMap(pm);
    }

    @Override
    public Collection<String> getParameterMapNames() {
        return this.originConfiguration.getParameterMapNames();
    }

    @Override
    public Collection<ParameterMap> getParameterMaps() {
        return this.originConfiguration.getParameterMaps();
    }

    @Override
    public ParameterMap getParameterMap(String id) {
        return this.originConfiguration.getParameterMap(id);
    }

    @Override
    public boolean hasParameterMap(String id) {
        return this.originConfiguration.hasParameterMap(id);
    }

    @Override
    public void addMappedStatement(MappedStatement ms) {
        this.originConfiguration.addMappedStatement(ms);
    }

    @Override
    public Collection<String> getMappedStatementNames() {
        return this.originConfiguration.getMappedStatementNames();
    }

    @Override
    public Collection<MappedStatement> getMappedStatements() {
        return this.originConfiguration.getMappedStatements();
    }

    @Override
    public Collection<XMLStatementBuilder> getIncompleteStatements() {
        return this.originConfiguration.getIncompleteStatements();
    }

    @Override
    public void addIncompleteStatement(XMLStatementBuilder incompleteStatement) {
        this.originConfiguration.addIncompleteStatement(incompleteStatement);
    }

    @Override
    public Collection<CacheRefResolver> getIncompleteCacheRefs() {
        return this.originConfiguration.getIncompleteCacheRefs();
    }

    @Override
    public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef) {
        this.originConfiguration.addIncompleteCacheRef(incompleteCacheRef);
    }

    @Override
    public Collection<ResultMapResolver> getIncompleteResultMaps() {
        return this.originConfiguration.getIncompleteResultMaps();
    }

    @Override
    public void addIncompleteResultMap(ResultMapResolver resultMapResolver) {
        this.originConfiguration.addIncompleteResultMap(resultMapResolver);
    }

    @Override
    public void addIncompleteMethod(MethodResolver builder) {
        this.originConfiguration.addIncompleteMethod(builder);
    }

    @Override
    public Collection<MethodResolver> getIncompleteMethods() {
        return this.originConfiguration.getIncompleteMethods();
    }

    @Override
    public MappedStatement getMappedStatement(String id) {
        return this.extEnhancer.getMappedStatement(id);
    }

    @Override
    public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
        return this.originConfiguration.getMappedStatement(id, validateIncompleteStatements);
    }

    @Override
    public Map<String, XNode> getSqlFragments() {
        return this.originConfiguration.getSqlFragments();
    }

    @Override
    public void addInterceptor(Interceptor interceptor) {
        this.originConfiguration.addInterceptor(interceptor);
    }

    @Override
    public void addMappers(String packageName, Class<?> superType) {
        this.originConfiguration.addMappers(packageName, superType);
    }

    @Override
    public void addMappers(String packageName) {
        this.originConfiguration.addMappers(packageName);
    }

    @Override
    public <T> void addMapper(Class<T> type) {
        this.originConfiguration.addMapper(type);
    }

    @Override
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        return this.originConfiguration.getMapper(type, sqlSession);
    }

    @Override
    public boolean hasMapper(Class<?> type) {
        return this.originConfiguration.hasMapper(type);
    }

    @Override
    public boolean hasStatement(String statementName) {
        return this.extEnhancer.hasStatement(statementName);
    }

    @Override
    public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {
        return this.originConfiguration.hasStatement(statementName, validateIncompleteStatements);
    }

    @Override
    public void addCacheRef(String namespace, String referencedNamespace) {
        this.originConfiguration.addCacheRef(namespace, referencedNamespace);
    }

    public void validateAllMapperMethod() {
        this.extEnhancer.validateAllMapperMethod();
    }

}
