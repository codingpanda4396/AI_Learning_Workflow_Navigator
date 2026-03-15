package com.pandanav.learning.api.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pandanav.learning.domain.model.CompletionRule;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LearningStepResponse")
public record LearningStepResponse(
    @JsonProperty("step_id")
    @Schema(name = "step_id", example = "9001")
    Long stepId,
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1003")
    Long taskId,
    @Schema(example = "TRAINING")
    String stage,
    @Schema(example = "QUESTION")
    String type,
    @JsonProperty("step_order")
    @Schema(name = "step_order", example = "1")
    Integer stepOrder,
    @Schema(example = "ACTIVE")
    String status,
    @Schema(example = "完成训练题 1")
    String objective,
    @JsonProperty("completion_rule")
    CompletionRule completionRule
) {
}

