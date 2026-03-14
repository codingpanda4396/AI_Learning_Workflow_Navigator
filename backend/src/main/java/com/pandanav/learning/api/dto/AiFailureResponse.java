package com.pandanav.learning.api.dto;

public record AiFailureResponse(
    boolean success,
    String code,
    String message,
    boolean retryable,
    String traceId
) {
}
