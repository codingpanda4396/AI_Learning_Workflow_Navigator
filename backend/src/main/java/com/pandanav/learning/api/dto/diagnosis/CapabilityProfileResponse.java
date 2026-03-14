package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CapabilityProfileResponse(
    @JsonProperty("sessionId")
    Long sessionId,
    @JsonProperty("capabilityProfile")
    CapabilityProfileDto capabilityProfile,
    @JsonProperty("insights")
    DiagnosisInsightsDto insights
) {
}
