package com.pandanav.learning.domain.llm.model;

public record LlmCallResultMeta(
    boolean success,
    boolean fallbackApplied,
    LlmFallbackReason fallbackReason,
    LlmFailureType failureType
) {
}
