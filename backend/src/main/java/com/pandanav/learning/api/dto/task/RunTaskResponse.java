package com.pandanav.learning.api.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "RunTaskResponse")
public record RunTaskResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1002")
    Long taskId,
    @Schema(example = "UNDERSTANDING")
    String stage,
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @Schema(example = "SUCCEEDED")
    String status,
    @JsonProperty("generation_mode")
    @Schema(name = "generation_mode", example = "LLM")
    String generationMode,
    @JsonProperty("generation_reason")
    @Schema(name = "generation_reason", example = "LLM request timed out")
    String generationReason,
    JsonNode output,
    @JsonProperty("current_step")
    LearningStepResponse currentStep,
    @JsonProperty("next_step_hint")
    String nextStepHint,
    List<LearningStepResponse> steps
) {
}
