package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record PreviewLearningPlanRequest(
    @NotBlank
    @JsonProperty("goalId")
    String goalId,
    @NotBlank
    @JsonProperty("diagnosisId")
    String diagnosisId,
    @JsonAlias("courseId")
    @JsonProperty("courseName")
    String courseName,
    @JsonAlias("chapterId")
    @JsonProperty("chapterName")
    String chapterName,
    @JsonProperty("goalText")
    String goalText,
    @Valid
    @JsonProperty("adjustments")
    LearningPlanAdjustmentsRequest adjustments
) {
}
