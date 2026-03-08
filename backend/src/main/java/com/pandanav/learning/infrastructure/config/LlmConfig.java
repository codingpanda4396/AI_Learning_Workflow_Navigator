package com.pandanav.learning.infrastructure.config;

import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.infrastructure.external.llm.DisabledLlmGateway;
import com.pandanav.learning.infrastructure.external.llm.OpenAiCompatibleLlmGateway;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    @Bean
    public LlmGateway llmGateway(LlmProperties properties, RestClient.Builder restClientBuilder) {
        if (!properties.isReady()) {
            return new DisabledLlmGateway();
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeoutMs());
        requestFactory.setReadTimeout(properties.getTimeoutMs());

        RestClient restClient = restClientBuilder
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();

        return new OpenAiCompatibleLlmGateway(restClient, properties);
    }
}
