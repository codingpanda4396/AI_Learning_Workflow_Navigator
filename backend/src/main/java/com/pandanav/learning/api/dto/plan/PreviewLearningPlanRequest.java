package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record PreviewLearningPlanRequest(
    @NotBlank
    @JsonProperty("goalId")
    String goalId,
    @NotBlank
    @JsonProperty("diagnosisId")
    String diagnosisId,
    @JsonProperty("courseId")
    String courseId,
    @JsonProperty("chapterId")
    String chapterId,
    @JsonProperty("goalText")
    String goalText,
    @Valid
    @JsonProperty("adjustments")
    LearningPlanAdjustmentsRequest adjustments
) {
}
