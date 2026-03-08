package com.pandanav.learning.domain.llm.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.domain.enums.Stage;

public record StageContent(
    Stage stage,
    JsonNode content,
    String generationMode,
    String promptVersion,
    String provider,
    String model,
    LlmUsage usage
) {
}

