package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiagnosisQuestionOptionDto(
    @JsonProperty("code")
    String code,
    @JsonProperty("label")
    String label,
    @JsonProperty("order")
    Integer order
) {
}
