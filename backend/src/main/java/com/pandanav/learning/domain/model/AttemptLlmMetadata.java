package com.pandanav.learning.domain.model;

public record AttemptLlmMetadata(
    String llmProvider,
    String llmModel,
    String promptVersion,
    Integer tokenInput,
    Integer tokenOutput,
    Integer latencyMs,
    String generationMode
) {

    public static AttemptLlmMetadata none(String generationMode) {
        return new AttemptLlmMetadata(null, null, null, null, null, null, generationMode);
    }
}

