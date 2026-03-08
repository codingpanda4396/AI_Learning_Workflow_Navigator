package com.pandanav.learning.domain.llm.model;

public record LlmUsage(
    Integer tokenInput,
    Integer tokenOutput,
    Integer latencyMs
) {
}

