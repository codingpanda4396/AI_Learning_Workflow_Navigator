package com.pandanav.learning.api.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdvanceStepResponse")
public record AdvanceStepResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1003")
    Long taskId,
    @JsonProperty("current_step")
    LearningStepResponse currentStep,
    @JsonProperty("next_step")
    LearningStepResponse nextStep,
    @JsonProperty("next_step_hint")
    @Schema(name = "next_step_hint", example = "继续下一步：完成训练题 2")
    String nextStepHint
) {
}

