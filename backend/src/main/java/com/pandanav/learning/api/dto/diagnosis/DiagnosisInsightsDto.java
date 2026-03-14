package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DiagnosisInsightsDto(
    @JsonProperty("summary")
    String summary,
    @JsonProperty("planExplanation")
    String planExplanation
) {
}
