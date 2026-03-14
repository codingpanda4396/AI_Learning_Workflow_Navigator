package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SubmitDiagnosisSessionResponse(
    @JsonProperty("diagnosisId")
    Long diagnosisId,
    @JsonProperty("sessionId")
    Long sessionId,
    @JsonProperty("status")
    String status,
    @JsonProperty("capabilityProfile")
    CapabilityProfileDto capabilityProfile,
    @JsonProperty("insights")
    DiagnosisInsightsDto insights,
    @JsonProperty("nextAction")
    DiagnosisNextActionDto nextAction,
    @JsonProperty("fallback")
    DiagnosisFallbackDto fallback,
    @JsonProperty("metadata")
    DiagnosisMetadataDto metadata
) {
}
