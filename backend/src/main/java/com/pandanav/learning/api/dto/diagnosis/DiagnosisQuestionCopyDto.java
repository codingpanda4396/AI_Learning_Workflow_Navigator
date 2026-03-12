package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DiagnosisQuestionCopyDto(
    @JsonProperty("sectionLabel")
    String sectionLabel,
    @JsonProperty("title")
    String title,
    @JsonProperty("description")
    String description,
    @JsonProperty("placeholder")
    String placeholder,
    @JsonProperty("submitHint")
    String submitHint
) {
}
