package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiagnosisAnswerSubmissionDto(
    @NotBlank
    @JsonProperty("questionId")
    String questionId,
    @JsonProperty("selectedOptionCode")
    String selectedOptionCode,
    @JsonProperty("selectedOptionCodes")
    @JsonAlias("answerCodes")
    List<String> selectedOptionCodes,
    @JsonProperty("text")
    @JsonAlias("answerText")
    String text,
    @JsonAlias("value")
    @JsonProperty("value")
    JsonNode legacyValue
) {
}
