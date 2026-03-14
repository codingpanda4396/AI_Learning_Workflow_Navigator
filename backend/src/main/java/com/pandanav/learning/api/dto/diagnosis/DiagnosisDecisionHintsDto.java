package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DiagnosisDecisionHintsDto(
    @JsonProperty("planningFactors")
    List<String> planningFactors
) {
}
