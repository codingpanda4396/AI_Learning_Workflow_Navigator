package com.pandanav.learning.domain.llm.model;

public record LlmCallContext(
    String traceId,
    String requestId,
    LlmStage stage,
    String model
) {
}
