package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pandanav.learning.api.dto.CodeLabelDto;

public record LearningPlanAdjustmentsRequest(
    @JsonProperty("intensity")
    CodeLabelDto intensity,
    @JsonProperty("learningMode")
    CodeLabelDto learningMode,
    @JsonAlias("preferPrerequisite")
    @JsonProperty("prioritizeFoundation")
    Boolean prioritizeFoundation
) {
}
