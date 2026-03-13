package com.pandanav.learning.infrastructure.config;

import com.pandanav.learning.application.service.tutor.MockTutorProvider;
import com.pandanav.learning.application.service.tutor.RealTutorProvider;
import com.pandanav.learning.application.service.tutor.TutorProvider;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmFailureClassifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TutorLlmProperties.class)
public class TutorProviderConfig {

    @Bean
    public TutorProvider tutorProvider(
        TutorLlmProperties properties,
        RestClient.Builder restClientBuilder,
        PromptTemplateProvider promptTemplateProvider,
        LlmCallLogger llmCallLogger,
        LlmFailureClassifier llmFailureClassifier
    ) {
        if (!properties.isReady()) {
            return new MockTutorProvider();
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeoutMs());
        requestFactory.setReadTimeout(properties.getTimeoutMs());

        RestClient restClient = restClientBuilder
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();

        return new RealTutorProvider(restClient, properties, promptTemplateProvider, llmCallLogger, llmFailureClassifier);
    }
}
