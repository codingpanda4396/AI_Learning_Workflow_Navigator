package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Exposed diagnosis strategy for the create response (code, label, focuses).
 */
public record DiagnosisStrategyDto(
    @JsonProperty("code")
    String code,
    @JsonProperty("label")
    String label,
    @JsonProperty("focuses")
    List<String> focuses
) {
    public DiagnosisStrategyDto {
        focuses = focuses == null ? List.of() : List.copyOf(focuses);
    }
}
