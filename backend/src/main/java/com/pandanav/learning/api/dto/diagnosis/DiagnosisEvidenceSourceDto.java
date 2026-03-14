package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DiagnosisEvidenceSourceDto(
    @JsonProperty("label")
    String label,
    @JsonProperty("dimension")
    String dimension,
    @JsonProperty("sourceQuestionId")
    String sourceQuestionId
) {
}
