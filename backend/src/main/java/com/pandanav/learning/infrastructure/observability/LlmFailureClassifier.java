package com.pandanav.learning.infrastructure.observability;

import com.pandanav.learning.application.service.llm.LlmJsonParseException;
import com.pandanav.learning.domain.llm.model.LlmCallException;
import com.pandanav.learning.domain.llm.model.LlmFailureType;
import com.pandanav.learning.domain.llm.model.LlmFallbackReason;
import com.pandanav.learning.domain.service.LearningPlanSchemaValidationException;

import java.util.Locale;

public class LlmFailureClassifier {

    public LlmFailureType classifyFailure(Throwable error) {
        if (error instanceof LearningPlanSchemaValidationException) {
            return LlmFailureType.VALIDATION_ERROR;
        }
        if (error instanceof LlmJsonParseException) {
            return LlmFailureType.LLM_JSON_PARSE_ERROR;
        }
        if (error instanceof LlmCallException llmCallException) {
            return llmCallException.failureType();
        }
        String message = normalize(error == null ? null : error.getMessage());
        if (message.contains("timed out") || message.contains("timeout")) {
            return LlmFailureType.LLM_TIMEOUT;
        }
        if (message.contains("truncated") || message.contains("finishreason=length")) {
            return LlmFailureType.LLM_TRUNCATED;
        }
        if (message.contains("json")) {
            return LlmFailureType.LLM_JSON_PARSE_ERROR;
        }
        if (message.contains("empty response") || message.contains("no content")) {
            return LlmFailureType.EMPTY_RESPONSE;
        }
        if (message.contains("validation")) {
            return LlmFailureType.VALIDATION_ERROR;
        }
        if (message.contains("provider returned") || message.contains("http error") || message.contains("api")) {
            return LlmFailureType.LLM_API_ERROR;
        }
        return LlmFailureType.UNKNOWN_ERROR;
    }

    public LlmFallbackReason classifyFallback(Throwable error) {
        if (error instanceof LearningPlanSchemaValidationException schemaValidationException) {
            return schemaValidationException.fallbackReason();
        }
        if (error instanceof LlmJsonParseException jsonParseException) {
            return jsonParseException.fallbackReason();
        }
        return switch (classifyFailure(error)) {
            case LLM_TIMEOUT, TIMEOUT -> LlmFallbackReason.LLM_TIMEOUT;
            case LLM_API_ERROR, API_ERROR -> LlmFallbackReason.LLM_API_ERROR;
            case LLM_JSON_PARSE_ERROR, JSON_PARSE_ERROR -> LlmFallbackReason.JSON_PARSE_ERROR;
            case LLM_TRUNCATED -> LlmFallbackReason.OUTPUT_TRUNCATED;
            case EMPTY_RESPONSE -> LlmFallbackReason.EMPTY_RESPONSE;
            case VALIDATION_ERROR -> LlmFallbackReason.JSON_SCHEMA_MISMATCH;
            case UNKNOWN_ERROR -> LlmFallbackReason.UNKNOWN_ERROR;
        };
    }

    private String normalize(String message) {
        return message == null ? "" : message.toLowerCase(Locale.ROOT);
    }
}
