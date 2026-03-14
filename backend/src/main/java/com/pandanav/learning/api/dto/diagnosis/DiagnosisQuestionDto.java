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
    @JsonProperty("required")
    boolean required,
    @JsonProperty("options")
    List<DiagnosisQuestionOptionDto> options,
    @JsonProperty("title")
    String title,
    @JsonProperty("description")
    String description,
    @JsonProperty("placeholder")
    String placeholder,
    @JsonProperty("submitHint")
    String submitHint,
    @JsonProperty("sectionLabel")
    String sectionLabel
) {
}
