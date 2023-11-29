package io.github.mybatisext.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import io.github.mybatisext.ExtContext;

@Configuration
@ConfigurationProperties(prefix = MybatisExtProperties.PREFIX)
public class MybatisExtProperties {

    public static final String PREFIX = "mybatis-ext";

    /** 是否启用 */
    private boolean enabled = true;
    /** 是否启用级联 */
    private boolean cascadeEnabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCascadeEnabled() {
        return cascadeEnabled;
    }

    public void setCascadeEnabled(boolean cascadeEnabled) {
        this.cascadeEnabled = cascadeEnabled;
    }

    public ExtContext toExtContext() {
        ExtContext extContext = new ExtContext();
        extContext.setCascadeEnabled(cascadeEnabled);
        return extContext;
    }
}
