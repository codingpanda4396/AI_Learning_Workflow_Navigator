package com.pandanav.learning.infrastructure.config;

import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.infrastructure.external.llm.DisabledLlmGateway;
import com.pandanav.learning.infrastructure.external.llm.OpenAiCompatibleLlmGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean
    public LlmGateway llmGateway(LlmProperties properties, RestClient.Builder restClientBuilder) {
        if (!properties.isReady()) {
            log.warn(
                "LLM is NOT ready, using DisabledLlmGateway. Diagnosis/capability-profile will use fallback. " +
                "Check: app.llm.enabled={}, baseUrl={}, apiKey={}, model={}",
                properties.isEnabled(),
                properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank() ? "(set)" : "(empty)",
                properties.getApiKey() != null && !properties.getApiKey().isBlank() ? "(set)" : "(empty)",
                properties.getModel() != null && !properties.getModel().isBlank() ? properties.getModel() : "(empty)"
            );
            return new DisabledLlmGateway();
        }
        log.info("LLM is ready. Using OpenAiCompatibleLlmGateway with baseUrl={}", maskUrl(properties.getBaseUrl()));
        return new OpenAiCompatibleLlmGateway(restClientBuilder, properties);
    }

    private static String maskUrl(String url) {
        if (url == null || url.isBlank()) {
            return "(empty)";
        }
        int lastSlash = url.lastIndexOf('/');
        return lastSlash >= 0 ? url.substring(0, Math.min(lastSlash + 15, url.length())) + "..." : "(set)";
    }
}
