package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DiagnosisExplanationDto(
    @JsonProperty("whyTheseQuestions")
    String whyTheseQuestions,
    @JsonProperty("whatWillBeInferred")
    List<String> whatWillBeInferred,
    @JsonProperty("howItAffectsPlanning")
    String howItAffectsPlanning
) {
}
