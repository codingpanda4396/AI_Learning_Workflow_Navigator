package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateDiagnosisSessionResponse(
    @JsonProperty("diagnosisId")
    Long diagnosisId,
    @JsonProperty("sessionId")
    Long sessionId,
    @JsonProperty("status")
    String status,
    @JsonProperty("generationMode")
    String generationMode,
    @JsonProperty("questions")
    List<DiagnosisQuestionDto> questions,
    @JsonProperty("diagnosisExplanation")
    DiagnosisExplanationDto diagnosisExplanation,
    @JsonProperty("nextAction")
    DiagnosisNextActionDto nextAction,
    @JsonProperty("decisionHints")
    DiagnosisDecisionHintsDto decisionHints,
    @JsonProperty("fallback")
    DiagnosisFallbackDto fallback,
    @JsonProperty("metadata")
    DiagnosisMetadataDto metadata,
    @JsonProperty("learnerSnapshot")
    LearnerSnapshotDto learnerSnapshot,
    @JsonProperty("diagnosisStrategy")
    DiagnosisStrategyDto diagnosisStrategy,
    @JsonProperty("questionRationales")
    List<QuestionRationaleDto> questionRationales,
    @JsonProperty("personalizationMeta")
    PersonalizationMetaDto personalizationMeta
) {
}
