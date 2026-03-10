package com.pandanav.learning.domain.model;

public record LlmCallLog(
    Long taskAttemptId,
    String bizType,
    String invocationProfile,
    String provider,
    String model,
    String promptTemplateKey,
    String promptVersion,
    String requestPayload,
    String responsePayload,
    String parsedJson,
    String status,
    Integer latencyMs,
    Integer inputTokens,
    Integer outputTokens,
    Integer reasoningTokens,
    String finishReason,
    boolean timeoutFlag,
    boolean fallbackUsed,
    boolean parseSuccess,
    boolean schemaValid,
    boolean truncatedFlag
) {
}

