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
    DiagnosisMetadataDto metadata,
    @JsonProperty("reasoningSteps")
    java.util.List<DiagnosisReasoningStepDto> reasoningSteps,
    @JsonProperty("strengthSources")
    java.util.List<DiagnosisEvidenceSourceDto> strengthSources,
    @JsonProperty("weaknessSources")
    java.util.List<DiagnosisEvidenceSourceDto> weaknessSources,
    @JsonProperty("learnerProfileSnapshot")
    LearnerProfileStructuredSnapshotDto learnerProfileSnapshot
) {
}
