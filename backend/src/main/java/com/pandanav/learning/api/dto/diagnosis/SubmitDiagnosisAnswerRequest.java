package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitDiagnosisAnswerRequest(
    @NotBlank
    @JsonProperty("questionId")
    String questionId,
    @NotNull
    @JsonProperty("value")
    JsonNode value
) {
}
