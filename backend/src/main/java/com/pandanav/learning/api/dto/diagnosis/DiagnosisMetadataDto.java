package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiagnosisMetadataDto(
    @JsonProperty("questionCount")
    Integer questionCount,
    @JsonProperty("answerCount")
    Integer answerCount,
    @JsonProperty("profileVersion")
    Integer profileVersion
) {
}
