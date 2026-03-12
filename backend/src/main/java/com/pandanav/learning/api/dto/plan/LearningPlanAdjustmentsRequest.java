package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LearningPlanAdjustmentsRequest(
    @JsonProperty("intensity")
    String intensity,
    @JsonProperty("learningMode")
    String learningMode,
    @JsonProperty("preferPrerequisite")
    Boolean preferPrerequisite
) {
}
