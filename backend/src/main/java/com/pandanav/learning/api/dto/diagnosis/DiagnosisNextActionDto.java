package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DiagnosisNextActionDto(
    @JsonProperty("code")
    String code,
    @JsonProperty("label")
    String label
) {
}
