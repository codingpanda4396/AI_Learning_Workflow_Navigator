package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SubmitDiagnosisResponse(
    @JsonProperty("capabilityProfile")
    CapabilityProfileDto capabilityProfile,
    @JsonProperty("nextAction")
    DiagnosisNextActionDto nextAction
) {
}
