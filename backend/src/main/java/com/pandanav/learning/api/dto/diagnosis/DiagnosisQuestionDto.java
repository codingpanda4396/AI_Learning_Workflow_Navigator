package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DiagnosisQuestionDto(
    @JsonProperty("questionId")
    String questionId,
    @JsonProperty("dimension")
    String dimension,
    @JsonProperty("type")
    String type,
    @JsonProperty("title")
    String title,
    @JsonProperty("description")
    String description,
    @JsonProperty("options")
    List<String> options,
    @JsonProperty("required")
    boolean required
) {
}
