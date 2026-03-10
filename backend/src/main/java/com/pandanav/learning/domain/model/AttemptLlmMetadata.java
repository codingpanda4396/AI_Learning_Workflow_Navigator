package com.pandanav.learning.domain.model;

public record AttemptLlmMetadata(
    String llmProvider,
    String llmModel,
    String promptVersion,
    String invocationProfile,
    Integer tokenInput,
    Integer tokenOutput,
    Integer reasoningTokens,
    Integer latencyMs,
    String finishReason,
    boolean timeout,
    boolean truncated,
    String generationMode
) {

    public static AttemptLlmMetadata none(String generationMode) {
        return new AttemptLlmMetadata(null, null, null, null, null, null, null, null, false, false, generationMode);
    }
}

