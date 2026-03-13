package com.pandanav.learning.domain.llm.model;

public record LlmCallMetrics(
    int latencyMs,
    int promptTokens,
    int completionTokens,
    int totalTokens
) {
    public static LlmCallMetrics from(LlmUsage usage) {
        if (usage == null) {
            return new LlmCallMetrics(-1, -1, -1, -1);
        }
        return new LlmCallMetrics(
            usage.latencyMs() == null ? -1 : usage.latencyMs(),
            usage.tokenInput() == null ? -1 : usage.tokenInput(),
            usage.tokenOutput() == null ? -1 : usage.tokenOutput(),
            usage.totalTokens() == null ? -1 : usage.totalTokens()
        );
    }
}
