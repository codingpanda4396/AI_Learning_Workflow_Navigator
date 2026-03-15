package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record DiagnosisInsightsDto(
    @JsonProperty("summary")
    String summary,
    @JsonProperty("planExplanation")
    String planExplanation,
    @JsonProperty("featureSummary")
    Map<String, Object> featureSummary,
    @JsonProperty("strategyHints")
    Map<String, Object> strategyHints,
    @JsonProperty("constraints")
    Map<String, Object> constraints
) {

    public DiagnosisInsightsDto(String summary, String planExplanation) {
        this(summary, planExplanation, null, null, null);
    }
}
