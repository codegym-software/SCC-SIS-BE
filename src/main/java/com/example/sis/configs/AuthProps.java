package com.example.sis.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProps {

    private boolean autoAssignEnabled = true;
    private String defaultRoleCode = "SUPER_ADMIN";
    private Integer defaultCenterId;

    public boolean isAutoAssignEnabled() {
        return autoAssignEnabled;
    }

    public void setAutoAssignEnabled(boolean autoAssignEnabled) {
        this.autoAssignEnabled = autoAssignEnabled;
    }

    public String getDefaultRoleCode() {
        return defaultRoleCode;
    }

    public void setDefaultRoleCode(String defaultRoleCode) {
        this.defaultRoleCode = defaultRoleCode;
    }

    public Integer getDefaultCenterId() {
        return defaultCenterId;
    }

    public void setDefaultCenterId(Integer defaultCenterId) {
        this.defaultCenterId = defaultCenterId;
    }
}