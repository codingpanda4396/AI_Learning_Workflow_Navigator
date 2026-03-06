package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NextTaskResponse")
public record NextTaskResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1002")
    Long taskId,
    @Schema(example = "UNDERSTANDING")
    String stage,
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId
) {
}
