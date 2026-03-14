package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DiagnosisReasoningStepDto(
    @JsonProperty("dimension")
    String dimension,
    @JsonProperty("questionId")
    String questionId,
    @JsonProperty("questionTitle")
    String questionTitle,
    @JsonProperty("selectedAnswerLabel")
    String selectedAnswerLabel,
    @JsonProperty("inferredConclusion")
    String inferredConclusion
) {
}
