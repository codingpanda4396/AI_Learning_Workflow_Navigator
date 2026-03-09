package com.pandanav.learning.domain.llm.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

public record EvaluationResult(
    Integer score,
    BigDecimal normalizedScore,
    String feedback,
    List<String> errorTags,
    List<String> strengths,
    List<String> weaknesses,
    String suggestedNextAction,
    JsonNode rubric,
    JsonNode rawJson,
    String provider,
    String model,
    String promptKey,
    String promptVersion,
    LlmUsage usage
) {
}
