package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GenerateDiagnosisResponse(
    @JsonProperty("diagnosisId")
    Long diagnosisId,
    @JsonProperty("sessionId")
    Long sessionId,
    @JsonProperty("questions")
    List<DiagnosisQuestionDto> questions,
    @JsonProperty("fallbackApplied")
    Boolean fallbackApplied,
    @JsonProperty("fallbackReasons")
    List<String> fallbackReasons
) {
}
