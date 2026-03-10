package com.pandanav.learning.api.dto.practice;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record SessionQuizQuestionResponse(
    @JsonProperty("question_id")
    Long questionId,
    String type,
    String stem,
    JsonNode options,
    @JsonProperty("evaluation_focus")
    String evaluationFocus,
    String difficulty,
    String status
) {
}
