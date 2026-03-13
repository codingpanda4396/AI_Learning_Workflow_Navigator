package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.llm.model.LlmFallbackReason;
import com.pandanav.learning.infrastructure.exception.InternalServerException;

import java.util.List;

public class LearningPlanSchemaValidationException extends InternalServerException {

    private final List<String> errors;

    public LearningPlanSchemaValidationException(List<String> errors) {
        super("Learning plan JSON schema mismatch: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }

    public LlmFallbackReason fallbackReason() {
        return LlmFallbackReason.JSON_SCHEMA_MISMATCH;
    }
}
