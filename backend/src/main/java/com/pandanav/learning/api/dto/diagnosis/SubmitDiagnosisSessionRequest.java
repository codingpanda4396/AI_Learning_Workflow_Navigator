package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitDiagnosisSessionRequest(
    @Positive
    @JsonProperty("diagnosisId")
    Long diagnosisId,
    @Valid
    @NotEmpty
    @JsonProperty("answers")
    List<DiagnosisAnswerSubmissionDto> answers
) {
}
