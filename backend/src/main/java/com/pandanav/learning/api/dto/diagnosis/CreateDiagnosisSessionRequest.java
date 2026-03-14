package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateDiagnosisSessionRequest(
    @NotNull
    @Positive
    @JsonProperty("sessionId")
    Long sessionId
) {
}
