package com.pandanav.learning.infrastructure.observability;

import com.pandanav.learning.domain.llm.model.LlmFailureType;
import com.pandanav.learning.domain.llm.model.LlmFallbackReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmFailureClassifierTest {

    private final LlmFailureClassifier classifier = new LlmFailureClassifier();

    @Test
    void shouldMapJsonErrorsToJsonParseReason() {
        RuntimeException error = new RuntimeException("LLM provider response is not valid JSON.");

        assertEquals(LlmFailureType.JSON_PARSE_ERROR, classifier.classifyFailure(error));
        assertEquals(LlmFallbackReason.JSON_PARSE_ERROR, classifier.classifyFallback(error));
    }

    @Test
    void shouldMapEmptyResponseToEmptyResponseReason() {
        RuntimeException error = new RuntimeException("LLM provider returned empty response.");

        assertEquals(LlmFailureType.EMPTY_RESPONSE, classifier.classifyFailure(error));
        assertEquals(LlmFallbackReason.EMPTY_RESPONSE, classifier.classifyFallback(error));
    }
}
