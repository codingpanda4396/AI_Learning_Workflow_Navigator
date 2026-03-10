package com.pandanav.learning.domain.llm.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.domain.enums.Stage;

public record StageContent(
    Stage stage,
    JsonNode content,
    String generationMode,
    String promptKey,
    String promptVersion,
    LlmInvocationProfile invocationProfile,
    String provider,
    String model,
    LlmUsage usage,
    boolean parseSuccess,
    boolean schemaValid,
    boolean truncated,
    JsonNode requestPayload,
    JsonNode responsePayload
) {
}
