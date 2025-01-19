package io.github.mybatisext.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = MybatisExtProperties.PREFIX)
public class MybatisExtProperties {

    public static final String PREFIX = "mybatis-ext";

    /** 是否启用 */
    private boolean enabled = true;
    /** 默认启用过滤 */
    private boolean defaultFilterable = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDefaultFilterable() {
        return defaultFilterable;
    }

    public void setDefaultFilterable(boolean defaultFilterable) {
        this.defaultFilterable = defaultFilterable;
    }
}
