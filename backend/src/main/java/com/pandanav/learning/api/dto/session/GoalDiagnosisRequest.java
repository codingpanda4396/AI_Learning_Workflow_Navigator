package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "GoalDiagnosisRequest")
public record GoalDiagnosisRequest(
    @NotBlank
    @JsonProperty("course_id")
    String courseId,
    @NotBlank
    @JsonProperty("chapter_id")
    String chapterId,
    @NotBlank
    @JsonProperty("goal_text")
    String goalText
) {
}

