package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitDiagnosisAnswerRequest(
    @NotBlank
    @JsonProperty("questionId")
    String questionId,
    @JsonProperty("answerCodes")
    List<String> answerCodes,
    @JsonProperty("answerText")
    String answerText,
    @JsonAlias("value")
    @JsonProperty("value")
    JsonNode value
) {
}
