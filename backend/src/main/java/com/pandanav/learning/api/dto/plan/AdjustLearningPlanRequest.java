package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AdjustLearningPlanRequest(
    @JsonProperty("sessionId")
    @JsonAlias("learningSessionId")
    Long sessionId,
    @JsonProperty("previewId")
    @JsonAlias("planId")
    Long previewId,
    @NotBlank
    @JsonProperty("strategy")
    String strategy,
    @JsonProperty("reason")
    String reason,
    @JsonProperty("timeBudget")
    Integer timeBudget,
    @JsonProperty("userFeedback")
    String userFeedback
) {
}
