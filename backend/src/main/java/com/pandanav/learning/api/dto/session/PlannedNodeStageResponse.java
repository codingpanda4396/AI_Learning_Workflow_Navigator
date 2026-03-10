package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PlannedNodeStageResponse")
public record PlannedNodeStageResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1001")
    Long taskId,
    @Schema(example = "STRUCTURE")
    String stage,
    @Schema(example = "Build a structure map for concept: Context Switching")
    String objective,
    @Schema(example = "PENDING")
    String status
) {
}
