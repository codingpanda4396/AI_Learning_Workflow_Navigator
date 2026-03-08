package com.pandanav.learning.domain.llm.model;

import com.fasterxml.jackson.databind.JsonNode;

public record LlmTextResult(
    String text,
    String provider,
    String model,
    LlmUsage usage,
    JsonNode requestPayload,
    JsonNode responsePayload
) {
}

