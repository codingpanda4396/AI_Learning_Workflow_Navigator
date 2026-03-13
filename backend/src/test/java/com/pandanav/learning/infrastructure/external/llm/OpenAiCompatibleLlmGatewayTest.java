package com.pandanav.learning.infrastructure.external.llm;

import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class OpenAiCompatibleLlmGatewayTest {

    @Test
    void shouldTreatWrappedTimeoutAsRetryable() throws Exception {
        OpenAiCompatibleLlmGateway gateway = new OpenAiCompatibleLlmGateway(mock(RestClient.Builder.class), new LlmProperties());

        Method method = OpenAiCompatibleLlmGateway.class.getDeclaredMethod("isRetryable", Exception.class);
        method.setAccessible(true);

        boolean retryable = (boolean) method.invoke(
            gateway,
            new InternalServerException("LLM provider call timed out: I/O error on POST request: Read timed out")
        );

        assertTrue(retryable);
    }

    @Test
    void shouldNotTreatValidationErrorAsRetryable() throws Exception {
        OpenAiCompatibleLlmGateway gateway = new OpenAiCompatibleLlmGateway(mock(RestClient.Builder.class), new LlmProperties());

        Method method = OpenAiCompatibleLlmGateway.class.getDeclaredMethod("isRetryable", Exception.class);
        method.setAccessible(true);

        boolean retryable = (boolean) method.invoke(
            gateway,
            new InternalServerException("LLM provider returned 400 BAD_REQUEST")
        );

        assertFalse(retryable);
    }
}
