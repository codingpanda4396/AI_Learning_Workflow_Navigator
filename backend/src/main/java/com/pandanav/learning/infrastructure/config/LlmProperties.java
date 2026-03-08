package com.pandanav.learning.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    private boolean enabled;
    private String provider = "openai-compatible";
    private String baseUrl;
    private String apiKey;
    private String model;
    private int timeoutMs = 10000;
    private boolean fallbackToRule = true;
    private boolean logRequest = false;
    private boolean logResponse = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isFallbackToRule() {
        return fallbackToRule;
    }

    public void setFallbackToRule(boolean fallbackToRule) {
        this.fallbackToRule = fallbackToRule;
    }

    public boolean isLogRequest() {
        return logRequest;
    }

    public void setLogRequest(boolean logRequest) {
        this.logRequest = logRequest;
    }

    public boolean isLogResponse() {
        return logResponse;
    }

    public void setLogResponse(boolean logResponse) {
        this.logResponse = logResponse;
    }

    public boolean isReady() {
        return enabled
            && baseUrl != null && !baseUrl.isBlank()
            && apiKey != null && !apiKey.isBlank()
            && model != null && !model.isBlank();
    }
}

