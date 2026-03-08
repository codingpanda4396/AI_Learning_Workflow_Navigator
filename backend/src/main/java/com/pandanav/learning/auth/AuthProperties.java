package com.pandanav.learning.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String jwtSecret = "change-me-please-change-me-please-change-me";
    private int tokenExpireDays = 7;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public int getTokenExpireDays() {
        return tokenExpireDays;
    }

    public void setTokenExpireDays(int tokenExpireDays) {
        this.tokenExpireDays = tokenExpireDays;
    }
}
