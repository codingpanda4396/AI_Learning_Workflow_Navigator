package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TimelineItemResponse")
public record TimelineItemResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1001")
    Long taskId,
    @Schema(example = "STRUCTURE")
    String stage,
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @Schema(example = "SUCCEEDED")
    String status
) {
}
