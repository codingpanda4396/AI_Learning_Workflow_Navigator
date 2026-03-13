package com.pandanav.learning.infrastructure.observability;

import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmFallbackReason;
import com.pandanav.learning.domain.llm.model.LlmStage;

import java.time.Duration;
import java.time.Instant;

public final class LlmObservabilityHelper {

    private LlmObservabilityHelper() {
    }

    public static LlmCallContext context(LlmStage stage, String model) {
        return new LlmCallContext(TraceContext.traceId(), TraceContext.requestId(), stage, model);
    }

    public static int elapsedMs(Instant start) {
        return start == null ? -1 : (int) Duration.between(start, Instant.now()).toMillis();
    }

    public static LlmFallbackReason fallbackReason(String code) {
        if (code == null || code.isBlank()) {
            return LlmFallbackReason.UNKNOWN_ERROR;
        }
        try {
            return LlmFallbackReason.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return LlmFallbackReason.UNKNOWN_ERROR;
        }
    }
}
