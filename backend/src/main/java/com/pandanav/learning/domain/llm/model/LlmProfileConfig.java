package com.pandanav.learning.domain.llm.model;

import java.util.Map;

public record LlmProfileConfig(
    LlmInvocationProfile profile,
    String provider,
    String model,
    Integer timeoutMs,
    Integer maxTokens,
    Double temperature,
    boolean jsonResponse,
    boolean fallbackAllowed,
    boolean streamAllowed,
    Integer completionWarningThreshold,
    Map<String, Object> extraParams
) {
}
