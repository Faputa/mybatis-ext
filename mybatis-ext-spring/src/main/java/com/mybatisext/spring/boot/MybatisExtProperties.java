package com.mybatisext.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = MybatisExtProperties.PREFIX)
public class MybatisExtProperties {

    public static final String PREFIX = "mybatis-ext";

    /** 是否启用 */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
