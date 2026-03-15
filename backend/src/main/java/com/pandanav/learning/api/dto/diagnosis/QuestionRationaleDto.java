package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Per-question rationale for front-end "why this question" display.
 */
public record QuestionRationaleDto(
    @JsonProperty("questionId")
    String questionId,
    @JsonProperty("reasonLabel")
    String reasonLabel,
    @JsonProperty("personalizedWhy")
    String personalizedWhy
) {
}
