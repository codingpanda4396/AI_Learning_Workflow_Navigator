package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CreateDiagnosisSessionResponse(
    @JsonProperty("diagnosisId")
    Long diagnosisId,
    @JsonProperty("sessionId")
    Long sessionId,
    @JsonProperty("status")
    String status,
    @JsonProperty("questions")
    List<DiagnosisQuestionDto> questions,
    @JsonProperty("nextAction")
    DiagnosisNextActionDto nextAction,
    @JsonProperty("fallback")
    DiagnosisFallbackDto fallback,
    @JsonProperty("metadata")
    DiagnosisMetadataDto metadata
) {
}
