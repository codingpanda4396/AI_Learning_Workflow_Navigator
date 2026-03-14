package com.pandanav.learning.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private AiMode mode = AiMode.DEVELOPMENT;

    public AiMode getMode() {
        return mode;
    }

    public void setMode(AiMode mode) {
        this.mode = mode;
    }

    public boolean isCompetitionMode() {
        return mode == AiMode.COMPETITION;
    }
}
