package com.pandanav.learning.domain.model;

public record LlmCallLog(
    Long taskAttemptId,
    String bizType,
    String provider,
    String model,
    String promptTemplateKey,
    String promptVersion,
    String requestPayload,
    String responsePayload,
    String parsedJson,
    String status,
    Integer latencyMs,
    Integer tokenInput,
    Integer tokenOutput
) {
}

