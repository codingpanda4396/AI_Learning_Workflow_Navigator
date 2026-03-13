package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmitDiagnosisResponse(
    @JsonProperty("capabilityProfile")
    CapabilityProfileDto capabilityProfile,
    @JsonProperty("nextAction")
    DiagnosisNextActionDto nextAction,
    @JsonProperty("fallbackApplied")
    Boolean fallbackApplied,
    @JsonProperty("fallbackReasons")
    List<String> fallbackReasons,
    @JsonProperty("contentSource")
    String contentSource
) {
}
