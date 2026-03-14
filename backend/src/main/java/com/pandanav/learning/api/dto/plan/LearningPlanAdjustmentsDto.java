package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record LearningPlanAdjustmentsDto(
    @JsonProperty("intensity")
    String intensity,
    @JsonProperty("learningMode")
    String learningMode,
    @JsonAlias("preferPrerequisite")
    @JsonProperty("prioritizeFoundation")
    Boolean prioritizeFoundation
) {
}
