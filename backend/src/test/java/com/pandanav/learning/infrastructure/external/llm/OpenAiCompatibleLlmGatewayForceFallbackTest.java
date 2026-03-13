package com.pandanav.learning.infrastructure.external.llm;

import com.pandanav.learning.domain.llm.model.LlmFailureType;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmCallException;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmFailureClassifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class OpenAiCompatibleLlmGatewayForceFallbackTest {

    @Test
    void shouldShortCircuitWhenForceFallbackEnabled() {
        RestClient.Builder restClientBuilder = mock(RestClient.Builder.class);
        LlmProperties properties = new LlmProperties();
        properties.setForceFallback(true);
        properties.setModel("qwen3.5-plus");
        LlmCallLogger logger = new LlmCallLogger(mock(ObjectProvider.class));
        OpenAiCompatibleLlmGateway gateway = new OpenAiCompatibleLlmGateway(
            restClientBuilder,
            properties,
            logger,
            new LlmFailureClassifier()
        );

        LlmCallException error = assertThrows(
            LlmCallException.class,
            () -> gateway.generate(
                LlmStage.CAPABILITY_SUMMARY,
                new LlmPrompt(PromptTemplateKey.CAPABILITY_SUMMARY_V1, "CAPABILITY_SUMMARY", "v1", LlmInvocationProfile.LIGHT_JSON_TASK, "sys", "user", "{}", null, null, null)
            )
        );

        assertEquals(LlmFailureType.UNKNOWN_ERROR, error.failureType());
        verifyNoInteractions(restClientBuilder);
    }
}
