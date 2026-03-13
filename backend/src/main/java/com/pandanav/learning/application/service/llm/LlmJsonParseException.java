package com.pandanav.learning.application.service.llm;

import com.pandanav.learning.domain.llm.model.LlmFallbackReason;
import com.pandanav.learning.infrastructure.exception.InternalServerException;

public class LlmJsonParseException extends InternalServerException {

    private final LlmFallbackReason fallbackReason;
    private final String diagnosticSummary;

    public LlmJsonParseException(String message, LlmFallbackReason fallbackReason, String diagnosticSummary) {
        super(message);
        this.fallbackReason = fallbackReason;
        this.diagnosticSummary = diagnosticSummary;
    }

    public LlmFallbackReason fallbackReason() {
        return fallbackReason;
    }

    public String diagnosticSummary() {
        return diagnosticSummary;
    }
}
