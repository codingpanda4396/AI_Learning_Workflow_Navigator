package com.pandanav.learning.infrastructure.config;

import com.pandanav.learning.application.service.tutor.MockTutorProvider;
import com.pandanav.learning.application.service.tutor.RealTutorProvider;
import com.pandanav.learning.application.service.tutor.TutorProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TutorLlmProperties.class)
public class TutorProviderConfig {

    @Bean
    public TutorProvider tutorProvider(TutorLlmProperties properties, RestClient.Builder restClientBuilder) {
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

        return new RealTutorProvider(restClient, properties);
    }
}
