package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record PreviewLearningPlanRequest(
    @NotBlank
    @JsonProperty("diagnosisId")
    String diagnosisId,
    @JsonProperty("sessionId")
    Long sessionId,
    @NotBlank
    @JsonProperty("goalText")
    String goalText,
    @JsonAlias("courseId")
    @JsonProperty("courseName")
    String courseName,
    @JsonAlias("chapterId")
    @JsonProperty("chapterName")
    String chapterName,
    @Valid
    @JsonProperty("adjustments")
    LearningPlanAdjustmentsDto adjustments
) {
}
