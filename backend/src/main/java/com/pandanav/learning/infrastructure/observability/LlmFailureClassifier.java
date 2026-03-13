package com.pandanav.learning.infrastructure.observability;

import com.pandanav.learning.domain.llm.model.LlmCallException;
import com.pandanav.learning.domain.llm.model.LlmFailureType;
import com.pandanav.learning.domain.llm.model.LlmFallbackReason;

import java.util.Locale;

public class LlmFailureClassifier {

    public LlmFailureType classifyFailure(Throwable error) {
        if (error instanceof LlmCallException llmCallException) {
            return llmCallException.failureType();
        }
        String message = normalize(error == null ? null : error.getMessage());
        if (message.contains("timed out") || message.contains("timeout")) {
            return LlmFailureType.TIMEOUT;
        }
        if (message.contains("json")) {
            return LlmFailureType.JSON_PARSE_ERROR;
        }
        if (message.contains("empty response") || message.contains("no content")) {
            return LlmFailureType.EMPTY_RESPONSE;
        }
        if (message.contains("validation")) {
            return LlmFailureType.VALIDATION_ERROR;
        }
        if (message.contains("provider returned") || message.contains("http error") || message.contains("api")) {
            return LlmFailureType.API_ERROR;
        }
        return LlmFailureType.UNKNOWN_ERROR;
    }

    public LlmFallbackReason classifyFallback(Throwable error) {
        return switch (classifyFailure(error)) {
            case TIMEOUT -> LlmFallbackReason.LLM_TIMEOUT;
            case API_ERROR -> LlmFallbackReason.LLM_API_ERROR;
            case JSON_PARSE_ERROR -> LlmFallbackReason.JSON_PARSE_ERROR;
            case EMPTY_RESPONSE -> LlmFallbackReason.EMPTY_RESPONSE;
            case VALIDATION_ERROR -> LlmFallbackReason.MISSING_REQUIRED_FIELDS;
            case UNKNOWN_ERROR -> LlmFallbackReason.UNKNOWN_ERROR;
        };
    }

    private String normalize(String message) {
        return message == null ? "" : message.toLowerCase(Locale.ROOT);
    }
}
