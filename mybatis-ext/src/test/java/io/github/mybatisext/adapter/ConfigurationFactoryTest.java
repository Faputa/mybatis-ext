package io.github.mybatisext.adapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

class ConfigurationFactoryTest {

    @Test
    void createsConfigurationInterfaceProxiesForAllEntryPoints() {
        ExtContext extContext = new ExtContext();
        Configuration defaultConfiguration = ConfigurationFactory.create(extContext);
        Environment environment = new Environment("test", new JdbcTransactionFactory(), new UnpooledDataSource());
        Configuration environmentConfiguration = ConfigurationFactory.create(environment, extContext);

        assertTrue(defaultConfiguration instanceof ConfigurationInterface);
        assertTrue(environmentConfiguration instanceof ConfigurationInterface);
        assertNull(defaultConfiguration.getEnvironment());
        assertSame(environment, environmentConfiguration.getEnvironment());
    }

    @Test
    void delegatesConfigurationStateToTheOriginalInstance() {
        Configuration original = new Configuration();
        original.setCacheEnabled(false);
        Configuration proxy = ConfigurationFactory.create(original, new ExtContext());

        assertFalse(proxy.isCacheEnabled());
        proxy.setCacheEnabled(true);
        proxy.setMapUnderscoreToCamelCase(true);

        assertTrue(original.isCacheEnabled());
        assertTrue(original.isMapUnderscoreToCamelCase());
    }

    @Test
    void validatesAnEmptyMapperRegistry() {
        Configuration configuration = ConfigurationFactory.create(new ExtContext());

        assertDoesNotThrow(() -> ((ConfigurationInterface) configuration).validateAllMapperMethod());
    }
}
